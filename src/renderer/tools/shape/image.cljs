(ns renderer.tools.shape.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.utils.drop :as drop]
   [renderer.utils.file :as file]))

(derive :image ::tools/graphics)
(derive :image ::tools/box)

(defmethod tools/properties :image
  []
  {:icon "image"
   :description "The <image> SVG element includes images inside SVG documents. 
                 It can display raster image files or other SVG files."
   :attrs [:href]})

(defmethod tools/pointer-down :image
  [{:keys [adjusted-pointer-pos] :as db}]
  (file/open!
   {:startIn "pictures"
    :types [{:accept {"image/png" [".png"]
                      "image/jpeg" [".jpeg" ".jpg"]
                      "image/bmp" [".fmp"]}}]}
   (fn [file]
     (rf/dispatch [:set-tool :select])
     (drop/add-image! file adjusted-pointer-pos)))
  db)
