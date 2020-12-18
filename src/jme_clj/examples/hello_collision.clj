(ns jme-clj.examples.hello-collision
  "Clojure version of https://wiki.jmonkeyengine.org/docs/3.3/tutorials/beginner/hello_collision.html"
  (:require [clojure.string :as str]
            [jme-clj.core :refer :all])
  (:import
   (com.jme3.asset.plugins ZipLocator)
   (com.jme3.input KeyInput)
   (com.jme3.math ColorRGBA)))


(defn action-listener []
  (create-action-listener
   (fn [name pressed? tpf]
     (let [pressed-kw (-> name str/lower-case keyword)
           {:keys [player]} (get-state)]
       (if (= :jump pressed-kw)
         (when pressed?
           (call* player :jump (vec3 0 20 0)))
         (set-state pressed-kw pressed?))))))


(defn- set-up-keys []
  (apply-input-mapping
   {:triggers  {"Left"  (key-trigger KeyInput/KEY_A)
                "Right" (key-trigger KeyInput/KEY_D)
                "Up"    (key-trigger KeyInput/KEY_W)
                "Down"  (key-trigger KeyInput/KEY_S)
                "Jump"  (key-trigger KeyInput/KEY_SPACE)}
    :listeners {(action-listener) ["Left" "Right" "Up" "Down" "Jump"]}}))


(defn- set-up-light []
  (-> (light :ambient)
      (set* :color (.mult (ColorRGBA/White) 1.3))
      (add-light-to-root))
  (-> (light :directional)
      (set* :color ColorRGBA/White)
      (set* :direction (vec3 2.8 -2.8 -28 :normalize))
      (add-light-to-root)))


(defn init []
  (let [bullet-as     (bullet-app-state)
        ;bullet-as     (set* bullet-as :debug-enabled true)
        view-port     (set* (view-port) :background-color (color-rgba 0.7 0.8 1 1))
        fly-cam       (set* (fly-cam) :move-speed 100)
        _             (set-up-keys)
        _             (set-up-light)
        _             (register-locator "town.zip" ZipLocator)
        scene-model   (set* (load-model "main.scene") :local-scale (float 2))
        scene-shape   (create-mesh-shape scene-model)
        landscape     (rigid-body-control scene-shape 0)
        scene-model   (add-control scene-model landscape)
        capsule-shape (capsule-collision-shape 1.5 6 1)
        player        (-> (character-control capsule-shape 0.05)
                          (set* :jump-speed 20)
                          (set* :fall-speed 30)
                          (set* :gravity (vec3 0 -30 0))
                          (set* :physics-location (vec3 0 10 0)))]
    (attach bullet-as)
    (add-to-root scene-model)
    (-> bullet-as
        (get* :physics-space)
        (call* :add landscape))
    (-> bullet-as
        (get* :physics-space)
        (call* :add player))
    {:player         player
     :walk-direction (vec3)
     :left           false
     :right          false
     :up             false
     :down           false
     :cam-dir        (vec3)
     :cam-left       (vec3)}))


(defn simple-update [tpf]
  (let [{:keys [cam-dir
                cam-left
                walk-direction
                player
                left
                right
                up
                down] :as m} (get-state)
        cam-dir        (-> cam-dir (set-v3 (get* (cam) :direction)) (mult-local 0.6))
        cam-left       (-> cam-left (set-v3 (get* (cam) :left)) (mult-local 0.4))
        walk-direction (set-v3 walk-direction 0 0 0)
        direction      (cond
                         left cam-left
                         right (negate cam-left)
                         up cam-dir
                         down (negate cam-dir))
        walk-direction (or (some->> direction (add-v3-local walk-direction))
                           walk-direction)]
    ;;since we mutate objects internally, we don't need to return hash-map in here
    (set* player :walk-direction walk-direction)
    (set* (cam) :location (get* player :physics-location))))


(defsimpleapp app
              :init init
              :update simple-update)


(comment
 (start app)
 (stop app)

 (re-init app init)

 (unbind-app #'app))