(ns renderer.attribute.angle
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#angle"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as views]
   [renderer.components :as comp]))

(defmethod hierarchy/form-element ::angle
  [key value disabled? initial]
  [:<>
   [views/form-input {:key key
                      :value value
                      :disabled? disabled?
                      :placeholder initial}]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger {:asChild true}
     [:button.button.ml-px.level-2.text-muted
      {:style {:width "26px" :height "26px"}}
      [comp/icon "degrees" {:class "small"}]]]
    [:> Popover/Portal
     [:> Popover/Content {:sideOffset 5
                          :className "popover-content"
                          :align "end"}
      [:div.circular-slider]
      [:> Popover/Arrow {:class "popover-arrow"}]]]]])
