(ns renderer.effects
  (:require
   [platform :as platform]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.utils.data-transfer :as data-transfer]
   [renderer.utils.dom :as dom]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-fx
 :ipc-send
 (fn [[channel data]]
   (when platform/electron?
     (js/window.api.send channel (clj->js data)))))

(rf/reg-fx
 :ipc-invoke
 (fn [{:keys [channel data formatter on-resolution]}]
   (when platform/electron?
     (p/let [result (js/window.api.invoke channel (clj->js data))]
       (when on-resolution
         (rf/dispatch [on-resolution (cond-> result formatter formatter)]))))))

(rf/reg-fx
 :data-transfer
 (fn [[position data-transfer]]
   (data-transfer/items! position (.-items data-transfer))
   (data-transfer/files! position (.-files data-transfer))))

(rf/reg-fx
 :set-pointer-capture
 (fn [[target pointer-id]]
   (.setPointerCapture target pointer-id)))

(rf/reg-fx
 :local-storage-persist
 (fn [data]
   (local-storage/->store! data)))

(rf/reg-fx
 :clipboard-write
 (fn [[data]]
   (when data
     (js/navigator.clipboard.write
      (array (js/ClipboardItem.
              (let [blob-array (js-obj)]
                (doseq
                 [[type data]
                  [["image/svg+xml" data]
                   ["text/html" data]
                   ["text/plain" data]]]
                  (when (.supports js/ClipboardItem type)
                    (aset blob-array type (js/Blob. (array data) #js {:type type}))))
                blob-array)))))))

(rf/reg-fx
 :focus
 (fn [id]
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element))]
     (js/setTimeout #(.focus element)))))
