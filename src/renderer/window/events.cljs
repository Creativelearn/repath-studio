(ns renderer.window.events
  (:require
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.frame.events :as-alias frame.e]
   [renderer.window.effects :as fx]))

(rf/reg-event-db
 ::set-maximized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized? state)))

(rf/reg-event-db
 ::set-fullscreen
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen? state)))

(rf/reg-event-db
 ::set-minimized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized? state)))

(rf/reg-event-fx
 ::set-focused
 (rf/path :window)
 (fn [{:keys [db]} [_ state]]
   {:db (cond-> db
          :always
          (assoc :focused? state)

          state
          (assoc :focused-once? true))
    :fx [(when-not (:focused-once? db)
           [:dispatch-later {:ms 0 :dispatch [::frame.e/center]}])]}))

(rf/reg-event-fx
 ::close
 (fn [_ _]
   {::fx/close nil}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {:ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if platform/electron?
     {:ipc-send ["window-toggle-fullscreen"]}
     {::fx/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {:ipc-send ["window-minimize"]}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if platform/electron?
     {:ipc-send ["open-remote-url" url]}
     {::fx/open-remote-url url})))
