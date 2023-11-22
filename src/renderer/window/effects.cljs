(ns renderer.window.effects
  (:require
   [platform]
   [re-frame.core :as rf]))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-fx
 ::toggle-fullscreen
 (fn [_]
   (let [element js/document.documentElement]
     (if (.-fullscreenElement element)
       (.exitFullscreen element)
       (.requestFullscreen element)))))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (.open js/window url)))

(rf/reg-event-fx
 :window/close
 (fn [_ _]
   {::close nil}))

(rf/reg-event-fx
 :window/toggle-maximized
 (fn [_ _]
   {:send-to-main {:action "windowToggleMaximized"}}))

(rf/reg-event-fx
 :window/toggle-fullscreen
 (fn [_ _]
   (if platform/electron?
     {:send-to-main {:action "windowToggleFullscreen"}}
     {::toggle-fullscreen nil})))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   {:send-to-main {:action "windowMinimize"}}))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
   (if platform/electron?
     {:send-to-main {:action "openRemoteUrl" :data url}}
     {::open-remote-url url})))
