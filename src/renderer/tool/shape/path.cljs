(ns renderer.tool.shape.path
  "https://www.w3.org/TR/SVG/paths.html#PathElement"
  (:require
   ["paper" :refer [Path]]
   ["svg-path-bbox" :refer [svgPathBbox]]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :path ::tool.hierarchy/shape)

(defn manipulate-paper-path
  [path action options]
  (case action
    :simplify (.simplify path options)
    :smooth (.smooth path options)
    :flatten (.flatten path options)
    :reverse (.reverse path options)
    nil)
  path)

(defn manipulate
  [el action & more]
  (update-in el [:attrs :d] #(-> (Path. %)
                                 (manipulate-paper-path action more)
                                 (.exportSVG)
                                 (.getAttribute "d"))))

(defmethod tool.hierarchy/properties :path
  []
  {; :icon "bezier-curve"
   :description "The <path> SVG element is the generic element to define a shape.
                 All the basic shapes can be created with a path element."
   :icon "bezier-curve"
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :opacity]})

(defmethod tool.hierarchy/translate :path
  [el [x y]]
  (update-in el [:attrs :d] #(-> (svgpath %)
                                 (.translate x y)
                                 (.toString))))

(defmethod tool.hierarchy/scale :path
  [el ratio pivot-point]
  (let [[scale-x scale-y] ratio
        [x y] (tool.hierarchy/bounds el)
        [x y] (mat/sub (mat/add [x y]
                                (mat/sub pivot-point
                                         (mat/mul pivot-point ratio)))
                       (mat/mul ratio [x y]))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (.scale scale-x scale-y)
                                   (.translate x y)
                                   (.toString)))))

(defmethod tool.hierarchy/bounds :path
  [{{:keys [d]} :attrs}]
  (let [[left top right bottom] (js->clj (svgPathBbox d))]
    [left top right bottom]))

(defmethod tool.hierarchy/render-edit :path
  [el]
  (let [offset (element/offset el)
        segments (-> el :attrs :d svgpath .-segments)
        square-handle (fn [i [x y]]
                        ^{:key i}
                        [overlay/square-handle {:id (keyword (str i))
                                                :x x
                                                :y y
                                                :type :handle
                                                :tag :edit
                                                :element (:id el)}])]
    [:g {:key ::edit-handles}
     (map-indexed (fn [i segment]
                    (case (-> segment first str/lower-case)
                      "m"
                      (let [[x y] (mapv units/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      "l"
                      (let [[x y] (mapv units/unit->px [(second segment) (last segment)])
                            [x y] (mat/add offset [x y])]
                        (square-handle i [x y]))

                      nil))
                  segments)]))

(defn translate-segment
  [path i [x y]]
  (let [segment (aget (.-segments path) i)
        segment (array (aget segment 0)
                       (units/transform (aget segment 1) + x)
                       (units/transform (aget segment 2) + y))]
    (aset (.-segments path) i segment)
    path))

(defmethod tool.hierarchy/edit :path
  [el offset handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :d] #(-> (svgpath %)
                                   (translate-segment index offset)
                                   (.toString)))))
