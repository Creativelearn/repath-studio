(ns renderer.attribute.impl.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
   [renderer.ui :as ui]
   [renderer.utils.units :as units]))

(derive :x ::length)
(derive :y ::length)
(derive :x1 ::length)
(derive :y1 ::length)
(derive :x2 ::length)
(derive :y2 ::length)
(derive :cx ::length)
(derive :cy ::length)
(derive :dx ::length)
(derive :dy ::length)

(derive ::positive-length ::length)

(derive :width ::positive-length)
(derive :height ::positive-length)
(derive :stroke-width ::positive-length)
(derive :r ::positive-length)
(derive :rx ::positive-length)
(derive :ry ::positive-length)

(defmethod hierarchy/form-element [:default ::length]
  [_ k v {:keys [disabled placeholder]}]
  [:div.flex.w-full.gap-px
   [v/form-input k v
    {:disabled disabled
     :placeholder (if v placeholder "multiple")
     :on-wheel (fn [e]
                 (when (= (.-target e) (.-activeElement js/document))
                   (if (pos? (.-deltaY e))
                     (rf/dispatch [::element.e/update-attr-and-focus k - 1])
                     (rf/dispatch [::element.e/update-attr-and-focus k + 1]))))}]
   [:div.flex.gap-px
    [:button.form-control-button
     {:disabled disabled
      :on-pointer-down #(rf/dispatch [::element.e/update-attr k - 1])}
     [ui/icon "minus"]]
    [:button.form-control-button
     {:disabled disabled
      :on-click #(rf/dispatch [::element.e/update-attr k + 1])}
     [ui/icon "plus"]]]])

(defmethod hierarchy/update-attr ::length
  ([element attribute f arg1]
   (update-in element [:attrs attribute] units/transform f arg1))
  ([element attribute f arg1 arg2]
   (update-in element [:attrs attribute] units/transform f arg1 arg2))
  ([element attribute f arg1 arg2 arg3]
   (update-in element [:attrs attribute] units/transform f arg1 arg2 arg3))
  ([element attribute f arg1 arg2 arg3 & more]
   (update-in element [:attrs attribute] #(apply units/transform % f arg1 arg2 arg3 more))))

(defmethod hierarchy/update-attr ::positive-length
  ([element attribute f arg1]
   (update-in element [:attrs attribute] units/transform (fn [v] (max 0 (f v arg1)))))
  ([element attribute f arg1 arg2]
   (update-in element [:attrs attribute] units/transform (fn [v] (max 0 (f v arg1 arg2)))))
  ([element attribute f arg1 arg2 arg3]
   (update-in element [:attrs attribute] units/transform (fn [v] (max 0 (f v arg1 arg2 arg3)))))
  ([element attribute f arg1 arg2 arg3 & more]
   (update-in element [:attrs attribute] units/transform (fn [v] (max 0 (apply f v arg1 arg2 arg3 more))))))
