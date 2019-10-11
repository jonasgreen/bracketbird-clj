(ns bracketbird.config
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [recontain.core :as rc]
            [clojure.string :as string]
            [bracketbird.ui-services :as ui-services]
            [bracketbird.system :as system]
            [bracketbird.state :as state]
            [restyle.core :as rs]
            [bracketbird.util :as ut]
            [bracketbird.dom :as d]))

(defn- add-path [ctx hook & hks]
  (->> (conj hks hook)
       (reduce (fn [m h] (assoc m h (state/path h ctx))) {})))

(def ui-root {:hook          :ui-root
              :foreign-state (fn [ctx]
                               (add-path ctx :hook/system))

              :render        (fn [_]
                               (let [app-id (rc/fs [:hook/system :active-application])]
                                 [:div
                                  (if app-id
                                    [rc/container {:application-id app-id} :ui-application-page]
                                    [:div "No application"])]))})

(def ui-application-page {:hook          :ui-application-page
                          :ctx           [:application-id]
                          :local-state   (fn [_] {:active-page :ui-front-page})
                          :foreign-state (fn [ctx] (add-path ctx :hook/application))

                          :render        (fn [_]
                                           (condp = (rc/ls :active-page)
                                             :ui-front-page ^{:key 1} [rc/container {} :ui-front-page]
                                             :ui-tournament-page ^{:key 2} (let [tournament-id (-> (rc/fs [:hook/application :tournaments]) keys first)]
                                                                             [rc/container {:tournament-id tournament-id} :ui-tournament-page])
                                             [:div "page " (rc/ls :active-page) " not supported"]))})


(def ui-front-page {:hook              :ui-front-page
                    :ctx               [:application-id]

                    :render            (fn [h]
                                         [:div
                                          [:div {:style {:display :flex :justify-content :center :padding-top 30}}
                                           ;logo
                                           [:div {:style {:width 900}}
                                            [:div {:style {:letter-spacing 0.8 :font-size 22}}
                                             [:span {:style {:color "lightblue"}} "BRACKET"]
                                             [:span {:style {:color "#C9C9C9"}} "BIRD"]]]]

                                          [:div {:style {:display :flex :flex-direction :column :align-items :center}}
                                           [:div {:style {:font-size 48 :padding "140px 0 30px 0"}}
                                            "Instant tournaments"]
                                           [:button {:class    "largeButton primaryButton"
                                                     :on-click (fn [_] (rc/dispatch h :create-tournament))}

                                            "Create a tournament"]
                                           [:div {:style {:font-size 14 :color "#999999" :padding-top 6}} "No account required"]]])

                    :create-tournament (fn [h]
                                         (let [ctx (:ctx h)
                                               tournament-id (system/unique-id :tournament)]
                                           (ui-services/dispatch-event
                                             {:event-type     [:tournament :create]
                                              :ctx            (assoc ctx :tournament-id tournament-id)
                                              :content        {:tournament-id tournament-id}
                                              :state-coeffect #(-> (rc/update! % (rc/get-handle ctx :ui-application-page)
                                                                               assoc
                                                                               :active-page
                                                                               :ui-tournament-page))
                                              :post-render    (fn [_])})))})

(def ui-tournament-page {:hook                    :ui-tournament-page
                         :ctx                     [:application-id :tournament-id]
                         :local-state             (fn [_] {:items             {:teams    {:header "TEAMS" :content :ui-teams-tab}
                                                                               :settings {:header "SETTINGS" :content :ui-settings-tab}
                                                                               :matches  {:header "MATCHES" :content :ui-matches-tab}
                                                                               :ranking  {:header "SCORES" :content :ui-ranking-tab}}

                                                           :order             [:teams :settings :matches :ranking]
                                                           :selection-type    :single
                                                           :selected          :teams
                                                           :previous-selected :teams})

                         :render                  (fn [_]
                                                    (let [{:keys [items order]} (rc/ls)]
                                                      [::page
                                                       [::menu (map (fn [k] ^{:key k}
                                                                      [::menu-item {:current/item k
                                                                                    :events       [:click]} (get-in items [k :header])]) order)]
                                                       (->> items
                                                            (reduce-kv (fn [m k {:keys [content]}]
                                                                         (conj m ^{:key k} [::content-holder {:current/item k} [rc/container {} content]]))
                                                                       [])
                                                            seq)]))


                         [:page :style]           (fn [_] (rs/style
                                                            {:height         "100vh"
                                                             :display        :flex
                                                             :flex-direction :column}))

                         [:menu :style]           (fn [_] (rs/style
                                                            {:font-size      22
                                                             :display        :flex
                                                             :align-items    :center
                                                             :min-height     [:app-padding]
                                                             :padding-left   [:app-padding]
                                                             :letter-spacing 1.2
                                                             :padding-right  [:app-padding]}))

                         [:menu-item :style]      (fn [_] (rs/style
                                                            (merge
                                                              {:margin-right [:layout-unit]
                                                               :opacity      0.5
                                                               :cursor       :pointer}
                                                              (when (= (rc/ls :selected) (rc/ls :current/item)) {:opacity 1 :cursor :auto}))))

                         [:menu-item :on-click]   (fn [h _]
                                                    (rc/dispatch h :select-item (rc/ls :current/item)))


                         [:content-holder :style] (fn [_] (rs/style
                                                            (merge {:height :100%} (when-not (= (rc/ls :selected) (rc/ls :current/item))
                                                                                     {:display :none}))))

                         :select-item             (fn [h select]
                                                    (rc/put! h assoc :previous-selected (rc/ls :selected) :selected select))})

(def ui-teams-tab {:hook                 :ui-teams-tab
                   :ctx                  [:application-id :tournament-id]
                   :foreign-state        (fn [ctx] (add-path ctx :hook/teams-order :hook/teams))

                   :render               (fn [_]
                                           (let [{:keys [hook/teams-order hook/teams]} (rc/fs)]
                                             [::tab-content
                                              [::table {:events [:scroll]}
                                               (map (fn [team-id index]
                                                      ^{:key team-id} [rc/container {:team-id team-id} :ui-team-row index]) teams-order (range (count teams)))]

                                              ; input field
                                              [rc/container {} :ui-enter-team-input]]))

                   [:tab-content :style] (fn [_] (rs/style
                                                   (merge {:display        :flex
                                                           :flex-direction :column
                                                           :height         :100%}
                                                          (when (< 0 (rc/ls :table-scroll-top)) {:border-top [:border]}))))
                   [:table :style]       (fn [_] (rs/style
                                                   (merge {:padding-top    [:layout-unit]
                                                           :max-height     :100%
                                                           :min-height     :200px
                                                           :padding-bottom [:layout-unit]
                                                           :overflow-y     :auto}
                                                          (when (< 0 (rc/ls :table-scroll-bottom)) {:border-bottom [:border]}))))

                   :scroll-to-bottom     (fn [h] (-> h
                                                     (rc/get-dom-element :table)
                                                     (ut/scroll-elm-to-bottom!)))

                   :focus-last-team      (fn [{:keys [ctx]}]
                                           (when (seq (rc/fs :hook/teams-order))
                                             (-> (merge ctx {:team-id (last (rc/fs :hook/teams-order))})
                                                 (rc/get-handle :ui-team-row)
                                                 (rc/dispatch :focus))))})


(def ui-team-row {:hook                             :ui-team-row
                  :ctx                              [:application-id :tournament-id :team-id]
                  :foreign-state                    (fn [ctx] (add-path ctx :hook/team))
                  :local-state                      (fn [{:keys [hook/team]}]
                                                      {:input-delete-on-backspace? (clojure.string/blank? (:team-name team))})
                  :render                           (fn [_ index]
                                                      [::row {:events [:hover]}
                                                       [::icons {:events [:hover :click]}
                                                        [ut/icon (rc/bind-options {:id :delete-icon :events [:click]}) "clear"]]
                                                       [::space]
                                                       [::seeding (inc index)]
                                                       [::team-name {:elm    :input
                                                                     :type   :text
                                                                     :value  (or (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                                                     :events [:key :focus :change]}]])
                  [:row :style]                     (fn [_] (rs/style
                                                              {:display :flex :align-items :center :min-height [:row-height]}))
                  [:icons :style]                   (fn [_] (rs/style
                                                              {:display         :flex
                                                               :align-items     :center
                                                               :height          [:row-height]
                                                               :justify-content :center
                                                               :cursor          (if (rc/ls :icons-hover?) :pointer :normal)
                                                               :width           [:app-padding]}))

                  [:icons :on-click]                (fn [h _] (rc/dispatch h :delete-team))
                  [:delete-icon :on-click]          (fn [h _] (rc/dispatch h :delete-team))
                  [:delete-icon :style]             (fn [_] (rs/style
                                                              (merge {:font-size 8 :opacity 0.5 :transition "background 0.2s, color 0.2s, border-radius 0.2s"}
                                                                     (when-not (rc/ls :row-hover?)
                                                                       {:color :transparent})

                                                                     (when (rc/ls :icons-hover?)
                                                                       {:font-weight   :bold
                                                                        :background    :red
                                                                        :color         :white
                                                                        :font-size     10
                                                                        :border-radius 8}))))
                  [:space :style]                   (fn [_] (rs/style
                                                              {:width [:page-padding]}))
                  [:seeding :style]                 (fn [_] (rs/style
                                                              {:display :flex :align-items :center :width [:seeding-width] :opacity 0.5 :font-size 10}))
                  [:team-name :style]               (fn [_] (rs/style
                                                              {:border    :none
                                                               :padding   0
                                                               :min-width 200}))
                  [:team-name :on-key-down]         (fn [h e]
                                                      (d/handle-key e {:ESC            (fn [_] (rc/delete-local-state h) [:STOP-PROPAGATION])
                                                                       :ENTER          (fn [_] (rc/dispatch h :update-team))
                                                                       [:SHIFT :ENTER] (fn [_] (ui-services/dispatch-event
                                                                                                 {:event-type  [:team :create]
                                                                                                  :ctx         (:ctx h)
                                                                                                  :content     {:team-name ""
                                                                                                                :index     (ui-services/index-of h (rc/fs [:hook/team :team-id]))}
                                                                                                  :post-render (fn [event]
                                                                                                                 (-> (:ctx h)
                                                                                                                     (assoc :team-id (:team-id event))
                                                                                                                     (rc/get-handle :ui-team-row)
                                                                                                                     (rc/dispatch :focus)))}))
                                                                       :UP             (fn [_] (->> (rc/fs [:hook/team :team-id])
                                                                                                    (ui-services/previous-team h)
                                                                                                    (rc/focus h :ui-team-row :team-id)))
                                                                       :DOWN           (fn [_] (let [team-to-focus (ui-services/after-team h (rc/fs [:hook/team :team-id]))]
                                                                                                 (if team-to-focus
                                                                                                   (rc/focus h :ui-team-row :team-id team-to-focus)
                                                                                                   (rc/focus h :ui-enter-team-input))))}))
                  [:team-name :delete-on-backspace] (fn [h _] (rc/dispatch h :delete-team))
                  [:team-name :on-blur]             (fn [h _] (rc/dispatch h :update-team))

                  :update-team                      (fn [h]
                                                      (when (rc/has-changed (rc/ls :team-name-value) (rc/fs [:hook/team :team-name]))
                                                        (ui-services/dispatch-event
                                                          {:event-type [:team :update]
                                                           :ctx        (:ctx h)
                                                           :content    {:team-name (rc/ls :team-name-value)}})))
                  :delete-team                      (fn [h]
                                                      (let [team-id (rc/fs [:hook/team :team-id])
                                                            team-to-focus (or
                                                                            (ui-services/after-team h team-id)
                                                                            (ui-services/previous-team h team-id))]
                                                        (ui-services/dispatch-event
                                                          {:event-type  [:team :delete]
                                                           :ctx         (assoc (:ctx h) :team-id team-id)
                                                           :post-render (fn [_]
                                                                          (if team-to-focus
                                                                            (rc/focus h :ui-team-row :team-id team-to-focus)
                                                                            (rc/focus h :ui-enter-team-input)))})))
                  :focus                            (fn [h] (-> h (rc/get-dom-element :team-name) (.focus)))})

(def ui-enter-team-input {:hook                         :ui-enter-team-input
                          :ctx                          [:application-id :tournament-id]
                          :local-state                  (fn [_] {:input-delete-on-backspace? true})
                          :foreign-state                (fn [ctx] (add-path ctx :hook/teams))

                          :render                       (fn [_]
                                                          [::row
                                                           [::input {:placeholder "Enter team"
                                                                     :events      [:key :change]
                                                                     :type        :text
                                                                     :elm         :input
                                                                     :value       (rc/ls :input-value)}]
                                                           [::button {:class  "primaryButton"
                                                                      :events [:key :click]} "Add Team"]])
                          [:row :style]                 (fn [_] (rs/style
                                                                  {:padding-left [+ :app-padding :page-padding (when (seq (rc/fs :hook/teams)) :seeding-width)]
                                                                   :display      :flex
                                                                   :min-height   [:app-padding]
                                                                   :align-items  :center}))
                          [:input :style]               (fn [_] (rs/style
                                                                  {:border  :none
                                                                   :padding 0}))
                          [:input :on-key-down]         (fn [h e]
                                                          (d/handle-key e {[:ENTER] (fn [_] (rc/dispatch h :create-team) [:STOP-PROPAGATION :PREVENT-DEFAULT])
                                                                           [:UP]    (fn [_] (-> (:ctx h)
                                                                                                (rc/get-handle :ui-teams-tab)
                                                                                                (rc/dispatch :focus-last-team)))}))
                          [:input :delete-on-backspace] (fn [h _] (when-let [{:keys [team-name team-id]} (ui-services/last-team h)]
                                                                    (when (string/blank? team-name)
                                                                      (ui-services/dispatch-event
                                                                        {:event-type  [:team :delete]
                                                                         :ctx         (assoc (:ctx h) :team-id team-id)
                                                                         :post-render (fn [_]
                                                                                        (-> (:ctx h)
                                                                                            (rc/get-handle :ui-teams-tab)
                                                                                            (rc/dispatch :scroll-to-bottom)))}))))
                          [:button :on-click]           (fn [h _]
                                                          (rc/dispatch h :create-team)
                                                          (rc/dispatch h :focus))
                          [:button :on-key-down]        (fn [h e]
                                                          (d/handle-key e {[:ENTER] (fn [_]
                                                                                      (rc/dispatch h :create-team)
                                                                                      (rc/dispatch h :focus)
                                                                                      [:STOP-PROPAGATION :PREVENT-DEFAULT])}))
                          :did-mount                    (fn [h] (rc/dispatch h :focus))
                          :create-team                  (fn [{:keys [ctx] :as h}]
                                                          (ui-services/dispatch-event
                                                            {:event-type     [:team :create]
                                                             :ctx            ctx
                                                             :content        {:team-name (rc/ls :input-value)}
                                                             :state-coeffect #(-> % (rc/update! h dissoc :input-value))
                                                             :post-render    (fn [_]
                                                                               (-> (rc/get-handle ctx :ui-teams-tab)
                                                                                   (rc/dispatch :scroll-to-bottom)))}))
                          :focus                        (fn [h] (-> h (rc/get-dom-element :input) (.focus)))})

(def ui-settings-tab {:hook   :ui-settings-tab
                      :ctx    [:application-id :tournament-id]
                      :render (fn [_] [:div "settings tab"])})

(def ui-matches-tab {:hook   :ui-matches-tab
                     :ctx    [:application-id :tournament-id]
                     :render (fn [_] [:div "matches-tab"])})

(def ui-ranking-tab {:hook   :ui-ranking-tab
                     :ctx    [:application-id :tournament-id]
                     :render (fn [_] [:div "scores-tab"])})
