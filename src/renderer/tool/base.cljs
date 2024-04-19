(ns renderer.tool.base)

(derive ::renderable ::element)
(derive ::shape ::renderable)
(derive ::graphics ::renderable)
(derive ::gradient ::renderable)
(derive ::descriptive ::renderable)
(derive :foreignObject ::graphics)
(derive :textPath ::graphics)
(derive :tspan ::graphics)
(derive :linearGradient ::gradient)
(derive :radialGradient ::gradient)
(derive :desc ::descriptive)
(derive :metadata ::descriptive)
(derive :title ::descriptive)

;; Tool multimethods.
(defmulti pointer-down (fn [db _e] (:tool db)))
(defmulti pointer-move (fn [db _e] (:tool db)))
(defmulti pointer-up (fn [db _e] (:tool db)))
(defmulti double-click (fn [db _e] (:tool db)))
(defmulti drag (fn [db _e] (:tool db)))
(defmulti drag-start (fn [db _e] (:tool db)))
(defmulti drag-end (fn [db _e] (:tool db)))
(defmulti key-up (fn [db _e] (:tool db)))
(defmulti key-down (fn [db _e] (:tool db)))
(defmulti activate :tool)
(defmulti deactivate :tool)
(defmulti properties keyword)

(defmethod pointer-down :default [db _e] db)
(defmethod pointer-up :default [db _e] db)
(defmethod pointer-move :default [db _e] db)
(defmethod drag-start :default [db _e] db)
(defmethod double-click :default [db _e] db)
(defmethod key-up :default [db _e] db)
(defmethod key-down :default [db _e] db)
(defmethod drag :default [db e] (pointer-move db e))
(defmethod drag-end :default [db e] (pointer-up db e))
(defmethod activate :default [db] (assoc db :cursor "default"))
(defmethod deactivate :default [db] (assoc db :cursor "default"))
(defmethod properties :default [])

;; Element multimethods.
(defmulti render :tag)
(defmulti render-to-string :tag)
(defmulti path :tag)
(defmulti area :tag)
(defmulti centroid :tag)
(defmulti snapping-points :tag)
(defmulti render-edit :tag)
(defmulti bounds (fn [el _elements] (:tag el))) ; REVIEW
(defmulti translate (fn [el _offset] (:tag el)))
(defmulti position (fn [el _position] (:tag el)))
(defmulti scale (fn [el _ration _pivot-point] (:tag el)))
(defmulti edit (fn [el _offset _handle] (:tag el)))

(defmethod render :default [])
(defmethod render-to-string :default [element] [render element])
(defmethod render-edit :default [])
(defmethod bounds :default [])
(defmethod area :default [])
(defmethod centroid :default [])
(defmethod snapping-points :default [])
(defmethod path :default [element] element)
(defmethod scale :default [element] element)
(defmethod translate :default [element] element)
(defmethod position :default [element] element)
(defmethod edit :default [element] element)
