(ns renderer.core
  (:require
   #_["@sentry/electron/renderer" :as sentry-electron-renderer]
   #_["@sentry/react" :as sentry-react]
   ["electron-log/renderer"]
   ["paper" :refer [paper]]
   [cljs.reader :as edn]
   [config]
   [devtools.core :as devtools]
   [platform]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [reagent.dom :as ra.dom]
   [renderer.attribute.core]
   [renderer.db]
   [renderer.dialog.events]
   [renderer.dialog.subs]
   [renderer.document.events :as document.e]
   [renderer.document.subs]
   [renderer.element.events]
   [renderer.element.subs]
   [renderer.events]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.notification.events]
   [renderer.notification.subs]
   [renderer.reepl.core]
   [renderer.reepl.replumb :as replumb]
   [renderer.ruler.subs]
   [renderer.snap.events]
   [renderer.snap.subs]
   [renderer.subs]
   [renderer.timeline.events]
   [renderer.timeline.subs]
   [renderer.tool.core]
   [renderer.utils.dom :as dom]
   [renderer.utils.error :as error]
   [renderer.utils.keyboard :as keyb]
   [renderer.views :as v]
   [renderer.window.events :as window.e]
   [renderer.window.subs]
   [renderer.worker.events]
   [renderer.worker.subs]
   [replumb.repl :as repl]
   [shadow.cljs.bootstrap.browser :as bootstrap]
   [user]))

(def easter-egg "
██████╗░███████╗██████╗░░█████╗░████████╗██╗░░██╗
██╔══██╗██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║░░██║
██████╔╝█████╗░░██████╔╝███████║░░░██║░░░███████║
██╔══██╗██╔══╝░░██╔═══╝░██╔══██║░░░██║░░░██╔══██║
██║░░██║███████╗██║░░░░░██║░░██║░░░██║░░░██║░░██║
╚═╝░░╚═╝╚══════╝╚═╝░░░░░╚═╝░░╚═╝░░░╚═╝░░░╚═╝░░╚═╝

░██████╗████████╗██╗░░░██╗██████╗░██╗░█████╗░
██╔════╝╚══██╔══╝██║░░░██║██╔══██╗██║██╔══██╗
╚█████╗░░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
░╚═══██╗░░░██║░░░██║░░░██║██║░░██║██║██║░░██║
██████╔╝░░░██║░░░╚██████╔╝██████╔╝██║╚█████╔╝
╚═════╝░░░░╚═╝░░░░╚═════╝░╚═════╝░╚═╝░╚════╝░")

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (dom/root-element)]
    (ra.dom/unmount-component-at-node root-el)
    (ra.dom/render [error/boundary [v/root]] root-el)))

(defn bootstrap-cb
  []
  (replumb/run-repl "(in-ns 'user)" identity)
  (print "Welcome to your REPL!")
  (print "")
  (print "You can create or modify shapes using the command line.")
  (print "Type (help) to see a list of commands."))

(defn init-api!
  []
  (js/window.api.receive
   "fromMain"
   (fn [data]
     (case (.-action data)
       "fontsLoaded" (js/console.log "fontsLoaded")
       "windowMaximized" (rf/dispatch [::window.e/set-maximized? true])
       "windowUnmaximized" (rf/dispatch [::window.e/set-maximized? false])
       "windowEnteredFullscreen" (rf/dispatch [::window.e/set-fullscreen? true])
       "windowLeavedFullscreen" (rf/dispatch [::window.e/set-fullscreen? false])
       "windowMinimized" (rf/dispatch [::window.e/set-minimized? true])
       "windowRestored" (rf/dispatch [::window.e/set-minimized? false])
       "fileLoaded" (rf/dispatch [::document.e/load (edn/read-string (.-data data))])
       "fileSaved" (rf/dispatch [::document.e/saved (edn/read-string (.-data data))])))))

(defn load-system-fonts!
  []
  (let [fonts (js->clj js/window.api.systemFonts :keywordize-keys true)]
    (rf/dispatch-sync [:set-system-fonts fonts])))

(defn load-webref!
  []
  (p/let [files (js/window.api.webrefCss.listAll)]
    (rf/dispatch-sync [:set-webref-css (js->clj files :keywordize-keys true)])))

(defn ^:export init []
  #_(if platform/electron?
      (sentry-electron-renderer/init (clj->js config/sentry-options) sentry-react/init)
      (sentry-react/init (clj->js config/sentry-options)))

  (js/console.log (str "%c" easter-egg) "color: #e93976")

  (devtools/set-pref!
   :cljs-land-style
   (str "filter:invert(1);" (:cljs-land-style (devtools/get-prefs))))

  ;; https://code.thheller.com/blog/shadow-cljs/2017/10/14/bootstrap-support.html
  (bootstrap/init repl/st {:path "js/bootstrap" :load-on-init '[user]} bootstrap-cb)

  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:load-local-db])
  (rf/dispatch-sync [:theme/init-mode])
  (rf/dispatch-sync [::document.e/new])
  (rf/dispatch-sync [:set-tool :select])

  (rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (rf/dispatch-sync [::rp/set-keydown-rules keyb/keydown-rules])

  (.addEventListener js/document "keydown" keyb/event-handler)
  (.addEventListener js/document "keyup" keyb/event-handler)

  (.setup paper) ; REVIEW

  (if platform/electron?
    (do (load-system-fonts!)
        (load-webref!)
        (rf/dispatch-sync [:set-mdn (js->clj js/window.api.mdn :keywordize-keys true)])
        (init-api!))
    (.addEventListener js/document
                       "fullscreenchange"
                       #(rf/dispatch [:window.e/set-fullscreen? (boolean (.-fullscreenElement js/document))])))

  (mount-root))
