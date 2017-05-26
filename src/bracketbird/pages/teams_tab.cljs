(ns bracketbird.pages.teams-tab
  (:require [bracketbird.ui.styles :as s]
            [bracketbird.context :as context]
            [bracketbird.new-context :as new-context]
            [bracketbird.event-router :as event-router]
            [bracketbird.control.tournament-api :as tournament-api]
            [bracketbird.model.team :as t]
            [bracketbird.util :as ut]
            [utils.dom :as d]
            [bracketbird.tournament-controller :as t-ctrl]
            [bracketbird.pages.tournament-tab-content :as tab-content]
            [reagent.core :as r]
            [bracketbird.model.entity :as e]))


(defn move-entity-focus-up [entities entity sub-key]
  (-> (e/previous-entity entities entity)
      (ut/focus-by-entity sub-key)))

(defn move-entity-focus-down [entities entity sub-key]
  (-> (e/next-entity entities entity)
      (ut/focus-by-entity sub-key)))

(defn component-dispatcher [ctx enter-team-ctx]
  (let [config {:team-name  {:up     (fn [t] (move-entity-focus-up (t-ctrl/teams ctx) t :team-name))
                             :down   (fn [t] (when-not (move-entity-focus-down (t-ctrl/teams ctx) t :team-name)
                                               (ut/focus-by-ui-ctx enter-team-ctx :enter-team)))

                             :left   (fn [t] (ut/focus-by-entity t :team-id))
                             :update (fn [t team-name])
                             :inject (fn [t] (let [index (t-ctrl/index-of-team ctx t)]
                                               (t-ctrl/insert-team ctx "" index)))

                             :delete (fn [t] (let [next-focus-team (or (t-ctrl/next-team ctx t) (t-ctrl/previous-team ctx t))]
                                               (t-ctrl/delete-team ctx t)
                                               (if next-focus-team
                                                 (ut/focus-by-entity next-focus-team :team-name)
                                                 (ut/focus-by-ui-ctx enter-team-ctx :enter-team))))}


                :enter-team {:up          (fn []
                                            (when-let [last-team (t-ctrl/last-team ctx)]
                                              (ut/focus-by-entity last-team :team-name)))

                             :create-team (fn [team-name]
                                            (t-ctrl/add-team ctx team-name)
                                            (context/update-ui! enter-team-ctx ""))}}]

    (fn [path & args]
      (if-let [f (get-in config path)]
        (apply f args)
        (.warn js/console "Unable to dispatch " path " with " args)))))


(defn enter-team-component [{:keys [tournament-id] :as ctx}]
  (let [*ui-state (new-context/subscribe-ui ctx :enter-team-component)]
    (fn [old-ctx ctx]
      [:div {:style {:display :flex :margin-top 30 :padding-left 30 :align-items :center}}
       [:input {:placeholder "Enter team"
                :id          (new-context/dom-id ctx :enter-team-component)
                :type        :text
                :style       s/input-text-field
                :value       (:team-name @*ui-state)
                :on-key-down (d/handle-key {:ENTER #(tournament-api/create-team ctx (:team-name @*ui-state))})
                :on-change   (new-context/update-ui ctx :enter-team-component (fn[m] (println m)))}]

       [:button {:class "primaryButton"} "Add Team"]])))


(defn team-panel [position team _]
  (let [team-name-state (r/atom (t/team-name team))
        delete-by-backspace (atom (clojure.string/blank? (t/team-name team)))]
    (fn [position team dispatcher]

      [:div {:style {:display     :flex
                     :align-items :center
                     :min-height  30}}
       [:div {:style {:width 30 :opacity 0.5 :font-size 10}} (inc position)]
       [:input {:id          (ut/dom-id-from-entity team :team-name)
                :style       (merge s/input-text-field {:min-width 200})
                :value       @team-name-state


                #_(:on-key-down {:ENTER     {d/no-modifiers?   #(dispatcher [:team-name :down] team)
                                             d/shift-modifier? #(dispatcher [:team-name :inject] team)}
                                 :BACKSPACE {#(@delete-by-backspace) (dispatcher [:team-name :delete] team)}

                                 :DOWN      {d/no-modifiers? #(dispatcher [:team-name :down] team)}
                                 :UP        {d/no-modifiers? #(dispatcher [:team-name :up] team)}
                                 :ELSE      {#(not (clojure.string/blank? @team-name-state)) (reset! delete-by-backspace false)}})



                :on-key-down (fn [e]
                               (cond (and (d/key? :BACKSPACE e) @delete-by-backspace)
                                     (dispatcher [:team-name :delete] team)

                                     (d/key? :ENTER e)
                                     (do
                                       (.stopPropagation e)
                                       (.preventDefault e)
                                       (dispatcher [:team-name :down] team))

                                     (d/key-and-modifier? :ENTER d/shift-modifier? e)
                                     (do
                                       (.stopPropagation e)
                                       (.preventDefault e)
                                       (dispatcher [:team-name :inject] team))

                                     (d/key? :DOWN e)
                                     (do (.stopPropagation e)
                                         (.preventDefault e)
                                         (dispatcher [:team-name :down] team))

                                     (d/key? :UP e)
                                     (dispatcher [:team-name :up] team)

                                     :else (when-not (clojure.string/blank? @team-name-state)
                                             (reset! delete-by-backspace false))))

                :on-key-up   (fn [e] (when (clojure.string/blank? @team-name-state)
                                       (reset! delete-by-backspace true)))

                :on-change   (fn [e] (reset! team-name-state (.. e -target -value)))}]])))

(defn teams-panel [ctx dispatcher teams]
  [:div
   (map-indexed (fn [i t] (ut/r-key t [team-panel i t dispatcher])) teams)])

(defn content [{:keys [tournament-id] :as model-ctx}]
  (let [teams (new-context/subscribe model-ctx :teams)]

    (fn [old-ctx ctx]
      (println "render teams" teams)
      [:div
       ;[teams-panel old-ctx dispatcher @teams]
       [enter-team-component ctx]])))

(defn render [old-ctx ctx]
  [tab-content/render old-ctx [content ctx]])