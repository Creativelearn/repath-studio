(ns renderer.element.effects
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element-handlers]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]))


(defn elements->string
  [elements]
  (reduce #(str % (tools/render-to-string %2)) "" elements))

(rf/reg-fx
 :clipboard-write
 (fn [text-html]
   (js/navigator.clipboard.write
    (clj->js [(js/ClipboardItem.
               #js {"text/html" (when text-html
                                  (js/Blob.
                                   [text-html]
                                   #js {:type ["text/html"]}))})]))))

(rf/reg-event-fx
 :element/copy
 (fn [{:keys [db]} [_]]
   (let [selected-elements (element-handlers/selected db)
         text-html (elements->string selected-elements)]
     {:db (element-handlers/copy db)
      :clipboard-write [text-html]})))

(rf/reg-event-fx
 :element/cut
 (fn [{:keys [db]} [_]]
   (let [selected-elements (element-handlers/selected db)
         text-html (elements->string selected-elements)]
     {:db (-> db
              (element-handlers/copy)
              (element-handlers/delete)
              (history/finalize "Cut selection"))
      :clipboard-write [text-html]})))
