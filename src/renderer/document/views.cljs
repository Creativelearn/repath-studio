(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [platform :as platform]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.history.events :as-alias history.e]
   [renderer.history.subs :as-alias history.s]
   [renderer.history.views :as history.v]
   [renderer.ui :as ui]))

(defn actions []
  (let [undos? @(rf/subscribe [::history.s/undos?])
        redos? @(rf/subscribe [::history.s/redos?])]
    [:div.toolbar

     [ui/icon-button
      "file"
      {:title "New"
       :on-click #(rf/dispatch [::document.e/new])}]

     [ui/icon-button
      "folder"
      {:title "Open"
       :on-click #(rf/dispatch [::document.e/open])}]

     [ui/icon-button
      "save"
      {:title "Save"
       :on-click #(rf/dispatch [::document.e/save])
       :disabled @(rf/subscribe [::document.s/active-saved?])}]

     [:span.v-divider]

     [:button.icon-button.items-center.px-1.gap-1
      {:title "Undo"
       :style {:margin-right 0
               :width "auto"
               :display "flex"}
       :on-click #(rf/dispatch [::history.e/undo])
       :disabled (not undos?)}
      [ui/icon "undo"]
      [history.v/select
       "Undo stack"
       @(rf/subscribe [::history.s/undos])
       (not undos?)]]

     [:button.icon-button.items-center.px-1.gap-1
      {:title "Redo"
       :style {:margin-right 0
               :width "auto"
               :display "flex"}
       :on-click #(rf/dispatch [::history.e/redo])
       :disabled (not redos?)}
      [ui/icon "redo"]
      [history.v/select
       "Redo stack"
       @(rf/subscribe [::history.s/redos])
       (not redos?)]]]))

(defn close-button
  [id saved?]
  [:button.close-document-button.small.hover:bg-transparent
   {:key (name id)
    :title "Close document"
    :on-pointer-down #(.stopPropagation %)
    :on-pointer-up (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [::document.e/close id true]))}
   [ui/icon "times"]
   (when-not saved?
     [ui/icon "dot" {:class "icon dot"}])])

(defn context-menu
  [id]
  (let [document @(rf/subscribe [::document.s/document id])
        path (:path document)
        document-tabs @(rf/subscribe [::app.s/document-tabs])]
    (cond-> [{:label "Close"
              :action [::document.e/close id true]}
             {:label "Close others"
              :action [::document.e/close-others id]
              :disabled? (empty? (rest document-tabs))}
             {:label "Close all"
              :action [::document.e/close-all]}
             {:label "Close saved"
              :action [::document.e/close-all-saved]}]
      platform/electron?
      (concat [{:type :separator}
               {:label "Open containing directory"
                :action [::document.e/open-directory path]
                :disabled? (not (and path platform/electron?))}]))))

(defn tab
  [id document active?]
  (ra/with-let [dragged-over? (ra/atom false)]
    (let [saved? @(rf/subscribe [::document.s/saved? id])]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
        [:div.document-tab
         {:class [(when active? "active")
                  (when saved? "saved")]
          :on-wheel #(rf/dispatch [::document.e/scroll (.-deltaY %)])
          :on-pointer-down #(case (.-buttons %)
                              4 (rf/dispatch [::document.e/close id true])
                              1 (rf/dispatch [::document.e/set-active id])
                              nil)
          :draggable true
          :on-drag-start #(.setData (.-dataTransfer %) "id" (name id))
          :on-drag-over #(.preventDefault %)
          :on-drag-enter #(reset! dragged-over? true)
          :on-drag-leave #(reset! dragged-over? false)
          :on-drop (fn [evt]
                     (.preventDefault evt)
                     (reset! dragged-over? false)
                     (rf/dispatch [::document.e/swap-position
                                   (-> (.-dataTransfer evt)
                                       (.getData "id")
                                       keyword)
                                   id]))}
         [:span.truncate.pointer-events-none
          (:title document)]
         [close-button id saved?]]]
       [:> ContextMenu/Portal
        (into
         [:> ContextMenu/Content
          {:class "menu-content context-menu-content"}]
         (map (fn [item]
                [ui/context-menu-item item])
              (context-menu id)))]])))

(defn tab-bar []
  (let [documents @(rf/subscribe [::app.s/documents])
        document-tabs @(rf/subscribe [::app.s/document-tabs])
        active-document @(rf/subscribe [::app.s/active-document])]
    [:div.flex.drag.justify-between
     [:div.flex.flex-1.overflow-hidden
      (for [document-id document-tabs]
        ^{:key (str document-id)}
        [tab document-id (get documents document-id) (= document-id active-document)])]
     [:div.toolbar
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:button.button.flex.items-center.justify-center.aria-expanded:overlay.px-2.font-mono.rounded
         [ui/icon "ellipsis-h"]]]
       [:> DropdownMenu/Portal
        [:> DropdownMenu/Content
         {:class "menu-content rounded"}
         (for [item [{:label "Close all"
                      :key :close-all
                      :action [::document.e/close-all]}
                     {:label "Close saved"
                      :key :close-all-saved
                      :action [::document.e/close-all-saved]}]]
           ^{:key (:key item)} [ui/dropdown-menu-item item])
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]]))
