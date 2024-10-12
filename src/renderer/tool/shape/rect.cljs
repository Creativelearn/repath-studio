(ns renderer.tool.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [clojure.string :as str]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :rect ::tool.hierarchy/box)
(derive :rect ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-alt"
   :description "The <rect> element is a basic SVG shape that draws rectangles,
                 defined by their position, width, and height. The rectangles
                 may have their corners rounded."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray
           :stroke-linejoin]})

(defmethod tool.hierarchy/help [:rect :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :rect
  [db e]
  (let [{:keys [stroke fill]} (get-in db [:documents (:active-document db)])
        [offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        lock-ratio (pointer/ctrl? e)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        attrs {:x (min x offset-x)
               :y (min y offset-y)
               :width (if lock-ratio (min width height) width)
               :height (if lock-ratio (min width height) height)
               :fill fill
               :stroke stroke}]
    (element.h/assoc-temp db {:type :element
                              :tag :rect
                              :attrs attrs})))

(defmethod tool.hierarchy/path :rect
  [el]
  (let [{{:keys [x y width height rx ry]} :attrs} el
        [x y width height] (mapv units/unit->px [x y width height])
        rx (units/unit->px (if (and (not rx) ry) ry rx))
        ry (units/unit->px (if (and (not ry) rx) rx ry))
        rx (if (> rx (/ width 2)) (/ width 2) rx)
        ry (if (> ry (/ height 2)) (/ height 2) ry)
        curved? (and (> rx 0) (> ry 0))]
    (->> ["M" (+ x rx) y
          "H" (- (+ x width) rx)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (+ x width) (+ y ry)]))
          "V" (- (+ y height) ry)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (- (+ x width) rx) (+ y height)]))
          "H" (+ x rx)
          (when curved? (str/join " " ["A" rx ry 0 0 1 x (- (+ y height) ry)]))
          "V" (+ y ry)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (+ x rx) y]))
          "z"]
         (remove nil?)
         (str/join " "))))
