(ns renderer.views
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.views :as attr]
   [renderer.components :as comp]
   [renderer.window.views :as win]
   [renderer.tree.views :as tree]
   [renderer.document.views :as doc]
   [renderer.tools.views :as tools]
   [renderer.frame.views :as frame]
   [renderer.rulers.views :as rulers]
   [renderer.object :as object]
   [renderer.codemirror.views :as cm]
   [renderer.reepl.views :as repl]
   [renderer.history.views :as history]
   [renderer.home :as home]
   [renderer.status :as status]
   [re-frame.registrar]
   [renderer.debug :as debug]
   [renderer.notification.views :as notification]
   ["@radix-ui/react-tooltip" :as Tooltip]))

(defn command-input []
  [:div.flex.flex-col.level-0.relative.overflow-visible
   {:style {:font-size "10px"
            :user-select "text"}}
   [repl/main-view]])

(defn editor []
  (let [rulers? @(rf/subscribe [:document/rulers?])]
    [:div.flex.flex-1.overflow-hidden
     [:div.flex.flex-col.flex-1.overflow-hidden
      [:div.flex.flex-col.flex-1.overflow-hidden
       [:div.flex.flex-1.overflow-hidden
        [:div.flex.flex-col.flex-1
         [:div.mb-px [tools/toolbar]
          (when rulers?
            [:div.flex.level-2
             [:div {:style {:width "23px" :height "23px"}}
              [comp/toggle-icon-button
               {:active? rulers?
                :active-icon "lock"
                :active-text "unlock"
                :inactive-icon "unlock"
                :inactive-text "lock"
                :class "small"
                :action #(rf/dispatch [:document/toggle-rulers-locked])}]]
             [:div.w-full.ml-px
              [rulers/ruler {:orientation :horizontal :size 23}]]])]
         [:div.flex.flex-1.relative
          [:<> (when rulers?
                 [:div.level-2.mr-px
                  [rulers/ruler {:orientation :vertical :size 23}]])]
          #_(when @(rf/subscribe [:command-palette?])
              [:div.command-palette
               (mapv (fn [command] {:key (keyword command)
                                    :text command
                                    :styles {:optionText {:font-size "14px"}}}) (keys (:event @re-frame.registrar/kind->id->handler)))])
          [:div.relative.grow.flex
           [frame/main]
           [:div.absolute.bottom-1.left-2.pointer-events-none
            {:style {:color "#555"}}
            @(rf/subscribe [:message])]
           (when @(rf/subscribe [:debug-info?])
             [:<>
              [debug/info]
              [debug/fps]])
           (when @(rf/subscribe [:backdrop?])
             [:div.backdrop
              {:on-click #(rf/dispatch [:set-backdrop false])}])]]]
        (when @(rf/subscribe [:document/xml?])
          (let [xml @(rf/subscribe [:elements/xml])]
            [:div.v-scroll.p-1.h-full.level-2.ml-px
             {:style {:flex "0 1 30%"}}
             [cm/editor xml {:options {:mode "text/xml"
                                       :readOnly true}}]]))
        (when @(rf/subscribe [:document/history?])
          [:div.v-scroll.p-1.level-2
           {:style {:flex "0 1 30%"}}])]
       [status/toolbar]
       [history/tree]]
      [command-input]]]))

(defn main-panel
  []
  [:> Tooltip/Provider
   [:div.flex.flex-col.flex-1.h-screen
    (when @(rf/subscribe [:window/header?]) [win/app-header])
    (if (seq @(rf/subscribe [:documents]))
      [:div.flex.flex-1.overflow-hidden
       {:on-drag-over #(rf/dispatch [:window/on-drag %])}
       (when @(rf/subscribe [:window/drag]) [:div.drag-overlay])
       (when @(rf/subscribe [:window/sidebar? :tree])
         [:div.flex.flex-col.mr-px
          {:style {:flex (str "0 0 " @(rf/subscribe [:window/sidebar :tree]) "px")}}
          [doc/actions]
          [tree/tree-sidebar]])
       [comp/resizer :tree :left]

       [:div.flex.flex-col.flex-1.overflow-hidden
        [doc/tab-bar]
        [:div.flex.flex-1.overflow-hidden
         [editor]
         [comp/resizer :properties :right]
         (when @(rf/subscribe [:window/sidebar? :properties])
           [:div.flex.flex-col.sidebar
            {:style {:flex (str "0 0 " @(rf/subscribe [:window/sidebar :properties]) "px")}}
            [:div.flex.flex-1.box-border.overflow-hidden
             [attr/form]]])
         [object/toolbar]]]]
      [home/panel])]

   [notification/main]])
