(ns renderer.tree.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.element.views :as element.v]
   [renderer.frame.events :as-alias frame.e]
   [renderer.tree.events :as-alias e]
   [renderer.ui :as ui]
   [renderer.utils.dom :as dom]
   [renderer.utils.keyboard :as keyb]))

(defn lock-button
  [id locked]
  [ui/icon-button
   (if locked "lock" "unlock")
   {:class (when-not locked "list-item-action")
    :title (if locked "unlock" "lock")
    :on-double-click dom/stop-propagation!
    :on-pointer-up dom/stop-propagation!
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.e/toggle-prop id :locked]))}])

(defn visibility-button
  [id visible]
  [ui/icon-button
   (if visible "eye" "eye-closed")
   {:class (when visible "list-item-action")
    :title (if visible "hide" "show")
    :on-double-click dom/stop-propagation!
    :on-pointer-up dom/stop-propagation!
    :on-click (fn [e]
                (.stopPropagation e)
                (rf/dispatch [::element.e/toggle-prop id :visible]))}])

(defn set-item-label!
  [e id]
  (rf/dispatch [::element.e/set-prop id :label (.. e -target -value)]))

(defn item-label
  [{:keys [id label visible tag]}]
  (ra/with-let [edit-mode? (ra/atom false)
                tag-label (str/capitalize (name tag))]
    (if @edit-mode?
      [:input.list-item-input
       {:default-value label
        :placeholder tag-label
        :auto-focus true
        :on-key-down #(keyb/input-key-down-handler! % label set-item-label! id)
        :on-blur (fn [e]
                   (reset! edit-mode? false)
                   (set-item-label! e id))}]
      [:div.flex
       [:div.truncate
        {:class [(when-not visible "opacity-60")
                 (when (= :svg tag) "font-bold")]
         :style {:cursor "text"}
         :on-double-click (fn [e]
                            (.stopPropagation e)
                            (reset! edit-mode? true))}
        (if (empty? label) tag-label label)]])))

(defn drop-handler!
  [e parent-id]
  (let [id (-> (.-dataTransfer e)
               (.getData "id")
               uuid)]
    (.preventDefault e)
    (rf/dispatch [::element.e/set-parent parent-id id])))
(defn padding
  [depth children?]
  (let [collapse-button-width 22]
    (- (* depth collapse-button-width)
       (if children? collapse-button-width 0))))

(def last-focused-id (ra/atom nil))

(defn set-last-focused-id!
  [id]
  (reset! last-focused-id id))

(defn key-down-handler!
  [e id]
  (case (.-key e)
    "ArrowUp"
    (do (.stopPropagation e)
        (rf/dispatch [::e/focus-up id]))

    "ArrowDown"
    (do (.stopPropagation e)
        (rf/dispatch [::e/focus-down id]))

    "ArrowLeft"
    (do (.stopPropagation e)
        (rf/dispatch [::document.e/collapse-el id]))

    "ArrowRight"
    (do (.stopPropagation e)
        (rf/dispatch [::document.e/expand-el id]))

    "Enter"
    (do (.stopPropagation e)
        (rf/dispatch [::element.e/select id (.-ctrlKey e)]))

    nil))

(defn toggle-collapsed-button
  [id collapsed]
  [ui/icon-button
   (if collapsed "chevron-right" "chevron-down")
   {:title (if collapsed "expand" "collapse")
    :on-pointer-up dom/stop-propagation!
    :on-click #(rf/dispatch (if collapsed
                              [::document.e/expand-el id]
                              [::document.e/collapse-el id]))}])

(defn list-item-button
  [{:keys [id selected children locked visible tag] :as el} depth hovered collapsed]
  [:div.button.list-item-button
   {:class [(when selected "selected")
            (when hovered "hovered")]
    :tab-index 0
    :data-id (str id)
    :role "menuitem"
    :on-double-click #(rf/dispatch [::frame.e/pan-to-element id])
    :on-pointer-enter #(rf/dispatch [::document.e/set-hovered-id id])
    :ref (fn [this]
           (when (and this selected)
             (dom/scroll-into-view! this)
             (set-last-focused-id! (.getAttribute this "data-id"))))
    :draggable true
    :on-key-down #(key-down-handler! % id)
    :on-drag-start #(-> (.-dataTransfer %)
                        (.setData "id" (str id)))
    :on-drag-enter #(rf/dispatch [::document.e/set-hovered-id id])
    :on-drag-over dom/prevent-default!
    :on-drop #(drop-handler! % id)
    :on-pointer-down #(when (= (.-button %) 2)
                        (rf/dispatch [::element.e/select id (.-ctrlKey %)]))
    :on-pointer-up (fn [e]
                     (.stopPropagation e)
                     (if (.-shiftKey e)
                       (rf/dispatch-sync [::e/select-range @last-focused-id id])
                       (do (rf/dispatch [::element.e/select id (.-ctrlKey e)])
                           (reset! last-focused-id id))))
    :style {:padding-left (padding depth (seq children))}}
   [:div.flex.items-center.content-between.w-full
    (when (seq children)
      [toggle-collapsed-button id collapsed])
    [:div.flex-1.overflow-hidden.flex.items-center
     {:class "gap-1.5"}
     [ui/icon
      (:icon (element.hierarchy/properties tag))
      {:class (when-not visible "opacity-60")}]
     [item-label el]]
    [lock-button id locked]
    [visibility-button id visible]]])

(defn item [{:keys [selected children id] :as el} depth elements hovered-ids collapsed-ids]
  (let [has-children? (seq children)
        collapsed (contains? collapsed-ids id)
        hovered (contains? hovered-ids id)]
    [:li {:class (when selected "overlay")}
     [list-item-button el depth hovered collapsed]
     (when (and has-children? (not collapsed))
       [:ul (for [el (mapv (fn [k] (get elements k)) (reverse children))]
              ^{:key (:id el)} [item el (inc depth) elements hovered-ids collapsed-ids])])]))

(defn inner-sidebar-render
  [root-children elements]
  (let [hovered-ids @(rf/subscribe [::document.s/hovered-ids])
        collapsed-ids @(rf/subscribe [::document.s/collapsed-ids])]
    [:div.tree-sidebar
     {:on-pointer-up #(rf/dispatch [::element.e/deselect-all])}
     [ui/scroll-area
      [:ul
       {:on-pointer-leave #(rf/dispatch [::document.e/clear-hovered])}
       (for [el (reverse root-children)]
         ^{:key (:id el)} [item el 1 elements hovered-ids collapsed-ids])]]]))

(defn inner-sidebar []
  (let [state @(rf/subscribe [::app.s/state])
        root-children @(rf/subscribe [::element.s/root-children])
        elements @(rf/subscribe [::document.s/elements])]
    (if (= state :default)
      [inner-sidebar-render root-children elements]
      (ra/with-let [root-children root-children
                    elements elements]
        [inner-sidebar-render root-children elements]))))

(defn root
  []
  [:> ContextMenu/Root
   [:> ContextMenu/Trigger
    {:class "flex h-full overflow-hidden"}
    [inner-sidebar]]
   [:> ContextMenu/Portal
    (into [:> ContextMenu/Content
           {:class "menu-content context-menu-content"
            :on-close-auto-focus dom/prevent-default!}]
          (map (fn [menu-item] [ui/context-menu-item menu-item])
               element.v/context-menu))]])
