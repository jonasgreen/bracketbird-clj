(ns bracketbird.components.settings-tab
  (:require [bracketbird.styles :as s]))


(defn render [{:keys[scroll-top]} foreign-state {:keys [ui-update]}]
  [:div {:style     (merge s/tournamet-tab-content-style (when (< 0 scroll-top) {:border-top "1px solid rgba(241,241,241,1)"}))
         :on-scroll (fn [e] (ui-update assoc :scroll-top (.-scrollTop (.-target e))))}
   [:div "assafasdf"]
   [:div "settings-ssss"]
   [:div "setings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "ccc"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "settings-ssss"]
   [:div "xxxx"]
   ])