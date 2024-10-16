(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :measure ::hierarchy/tool)

(defmethod hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod hierarchy/help [:measure :default]
  []
  "Click and drag to measure a distance.")

(defmethod hierarchy/activate :measure
  [db]
  (app.h/set-cursor db "crosshair"))

(defmethod hierarchy/pointer-up :measure
  [db]
  (element.h/dissoc-temp db))

(defmethod hierarchy/drag-end :measure
  [db] db)

(defmethod hierarchy/drag :measure
  [db]
  (let [{:keys [adjusted-pointer-offset adjusted-pointer-pos]} db
        [offset-x offset-y] adjusted-pointer-offset
        [x y] adjusted-pointer-pos
        [adjacent opposite] (mat/sub adjusted-pointer-offset adjusted-pointer-pos)
        hypotenuse (Math/hypot adjacent opposite)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke "gray"}]
    (element.h/assoc-temp db {:id :mesure
                              :type :overlay
                              :tag :measure
                              :attrs attrs
                              :hypotenuse hypotenuse})))
