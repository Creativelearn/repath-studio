(ns components.button-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]))

(defscene icon-buttons
  :title "Icon buttons"
  [:div.toolbar.bg-primary
   [ui/icon-button "download" {:title "download"
                               :on-click #(js/alert "Downloaded")}]
   [ui/icon-button "save" {:title "save"
                           :on-click #(js/alert "Saved")}]])

(defscene radio-icon-buttons
  :params (atom false)
  :title "Radio icon buttons"
  [store]
  [:div.toolbar.bg-primary
   [ui/radio-icon-button "refresh" @store
    {:title "Replay"
     :on-click #(swap! store not)}]])
