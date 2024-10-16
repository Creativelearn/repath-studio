(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :line ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-tool"})

(defn create-line
  [db]
  (let [stroke (get-in db [:documents (:active-document db) :stroke])
        [offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke stroke}]
    (element.h/assoc-temp db {:type :element
                              :tag :line
                              :attrs attrs})))

(defn update-line-end
  [db]
  (let [[x y] (:adjusted-pointer-pos db)
        temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x2] x)
                 (assoc-in [:attrs :y2] y))]
    (element.h/assoc-temp db temp)))

(defmethod tool.hierarchy/pointer-move :line
  [db]
  (cond-> db
    (element.h/get-temp db) (update-line-end)))

(defmethod tool.hierarchy/pointer-up :line
  [db _e]
  (cond
    (element.h/get-temp db)
    (-> db
        (element.h/add)
        (app.h/set-tool :select)
        (app.h/set-state :default)
        (app.h/explain "Create line"))

    (:pointer-offset db)
    (-> db
        (app.h/set-state :create)
        (create-line))

    :else db))

(defmethod tool.hierarchy/pointer-down :line
  [db _e]
  (cond-> db
    (element.h/get-temp db)
    (app.h/explain "Create line")))

(defmethod tool.hierarchy/drag :line
  [db]
  (create-line db))
