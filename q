[1mdiff --git a/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/SoloDraftRequestHandler.java b/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/SoloDraftRequestHandler.java[m
[1mindex c42d4e5c..a6ae538f 100644[m
[1m--- a/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/SoloDraftRequestHandler.java[m
[1m+++ b/gemp-swccg-async/src/main/java/com/gempukku/swccgo/async/handler/SoloDraftRequestHandler.java[m
[36m@@ -79,8 +79,10 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
         ((SoloDraftLeagueData) leagueData).repairExtraInformation(collection, resourceOwner);[m
 [m
         boolean finished = (Boolean) collection.getExtraInformation().get("finished");[m
[32m+[m[32m        int stage = ((Number) collection.getExtraInformation().get("stage")).intValue();[m
[32m+[m[32m        int stages = ((Number) collection.getExtraInformation().get("stageCount")).intValue();[m
[32m+[m[32m        String side = collection.getExtraInformation().get("draftSide").toString();[m
         if (!finished) {[m
[31m-            int stage = ((Number) collection.getExtraInformation().get("stage")).intValue();[m
             long playerSeed = ((Number) collection.getExtraInformation().get("seed")).longValue();[m
 [m
             SoloDraft soloDraft = soloDraftLeagueData.getSoloDraft();[m
[36m@@ -94,6 +96,13 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
         Document doc = documentBuilder.newDocument();[m
 [m
         Element availablePicksElem = doc.createElement("availablePicks");[m
[32m+[m
[32m+[m[32m        Element draftStateElem = doc.createElement("state");[m
[32m+[m[32m        draftStateElem.setAttribute("stage",String.valueOf(stage));[m
[32m+[m[32m        draftStateElem.setAttribute("stages",String.valueOf(stages));[m
[32m+[m[32m        draftStateElem.setAttribute("side",side);[m
[32m+[m[32m        availablePicksElem.appendChild(draftStateElem);[m
[32m+[m
         doc.appendChild(availablePicksElem);[m
 [m
         appendAvailablePics(doc, availablePicksElem, availableChoices);[m
[36m@@ -137,6 +146,8 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
             throw new HttpProcessingException(404);[m
 [m
         int stage = ((Number) collection.getExtraInformation().get("stage")).intValue();[m
[32m+[m[32m        int stages = ((Number) collection.getExtraInformation().get("stageCount")).intValue();[m
[32m+[m[32m        String side = collection.getExtraInformation().get("draftSide").toString();[m
         long playerSeed = ((Number) collection.getExtraInformation().get("seed")).longValue();[m
 [m
         SoloDraft soloDraft = soloDraftLeagueData.getSoloDraft();[m
[36m@@ -150,6 +161,7 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
         Map<String, Object> extraInformationChanges = new HashMap<String, Object>();[m
         boolean hasNextStage = soloDraft.hasNextStage(playerSeed, stage);[m
         extraInformationChanges.put("stage", stage + 1);[m
[32m+[m[32m        extraInformationChanges.put("draftSide", soloDraft.draftSide(stage));[m
         if (!hasNextStage)[m
             extraInformationChanges.put("finished", true);[m
 [m
[36m@@ -162,6 +174,13 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
         Document doc = documentBuilder.newDocument();[m
 [m
         Element pickResultElem = doc.createElement("pickResult");[m
[32m+[m
[32m+[m[32m        Element draftStateElem = doc.createElement("state");[m
[32m+[m[32m        draftStateElem.setAttribute("stage",String.valueOf(stage + 1));[m
[32m+[m[32m        draftStateElem.setAttribute("stages",String.valueOf(stages));[m
[32m+[m[32m        draftStateElem.setAttribute("side",side);[m
[32m+[m[32m        pickResultElem.appendChild(draftStateElem);[m
[32m+[m
         doc.appendChild(pickResultElem);[m
 [m
         for (CardCollection.Item item : selectedCards.getAll().values()) {[m
[36m@@ -184,12 +203,15 @@[m [mpublic class SoloDraftRequestHandler extends SwccgoServerRequestHandler implemen[m
             String choiceId = availableChoice.getChoiceId();[m
             String blueprintId = availableChoice.getBlueprintId();[m
             String choiceUrl = availableChoice.getChoiceUrl();[m
[32m+[m[32m            String packDesc = availableChoice.getObjPackDescription();[m
             Element availablePick = doc.createElement("availablePick");[m
             availablePick.setAttribute("id", choiceId);[m
             if (blueprintId != null)[m
                 availablePick.setAttribute("blueprintId", blueprintId);[m
             if (choiceUrl != null)[m
                 availablePick.setAttribute("url", choiceUrl);[m
[32m+[m[32m            if (packDesc != null)[m
[32m+[m[32m                availablePick.setAttribute("desc", packDesc);[m
             rootElem.appendChild(availablePick);[m
         }[m
     }[m
[1mdiff --git a/gemp-swccg-async/src/main/web/js/gemp-016/soloDraftUi.js b/gemp-swccg-async/src/main/web/js/gemp-016/soloDraftUi.js[m
[1mindex 4cfb62dc..fc3722fc 100644[m
[1m--- a/gemp-swccg-async/src/main/web/js/gemp-016/soloDraftUi.js[m
[1m+++ b/gemp-swccg-async/src/main/web/js/gemp-016/soloDraftUi.js[m
[36m@@ -88,6 +88,10 @@[m [mvar GempSwccgSoloDraftUI = Class.extend({[m
                 var root = xml.documentElement;[m
                 if (root.tagName == "availablePicks") {[m
                     var availablePicks = root.getElementsByTagName("availablePick");[m
[32m+[m[32m                    var draftState = root.getElementsByTagName("state")[0];[m
[32m+[m[32m                    var stage = draftState.getAttribute("stage");[m
[32m+[m[32m                    var stages = draftState.getAttribute("stages");[m
[32m+[m[32m                    var side = draftState.getAttribute("side");[m
                     for (var i = 0; i < availablePicks.length; i++) {[m
                         var availablePick = availablePicks[i];[m
                         var id = availablePick.getAttribute("id");[m
[36m@@ -109,10 +113,18 @@[m [mvar GempSwccgSoloDraftUI = Class.extend({[m
                         }[m
                     }[m
                     that.picksCardGroup.layoutCards();[m
[31m-                    if (availablePicks.length > 0)[m
[31m-                        that.messageDiv.text("Make a pick");[m
[31m-                    else[m
[32m+[m[32m                    if (availablePicks.length > 0) {[m
[32m+[m[32m                        if (side == "light") {[m
[32m+[m[32m                            side = "Light";[m
[32m+[m[32m                        }[m
[32m+[m[32m                        if (side == "dark") {[m
[32m+[m[32m                            side = "Dark";[m
[32m+[m[32m                        }[m
[32m+[m[32m                        that.messageDiv.text("Make a pick (stage " + stage + " / " + stages + " - " + side + ")");[m
[32m+[m[32m                    }[m
[32m+[m[32m                    else {[m
                         that.messageDiv.text("Draft is finished");[m
[32m+[m[32m                    }[m
                 }[m
             });[m
 [m
[36m@@ -177,6 +189,10 @@[m [mvar GempSwccgSoloDraftUI = Class.extend({[m
                                     that.draftedCardGroup.layoutCards();[m
 [m
                                     var availablePicks = root.getElementsByTagName("availablePick");[m
[32m+[m[32m                                    var draftState = root.getElementsByTagName("state")[0];[m
[32m+[m[32m                                    var stage = draftState.getAttribute("stage");[m
[32m+[m[32m                                    var stages = draftState.getAttribute("stages");[m
[32m+[m[32m                                    var side = draftState.getAttribute("side");[m
                                     for (var i = 0; i < availablePicks.length; i++) {[m
                                         var availablePick = availablePicks[i];[m
                                         var id = availablePick.getAttribute("id");[m
[36m@@ -198,10 +214,18 @@[m [mvar GempSwccgSoloDraftUI = Class.extend({[m
                                         }[m
                                     }[m
                                     that.picksCardGroup.layoutCards();[m
[31m-                                    if (availablePicks.length > 0)[m
[31m-                                        that.messageDiv.text("Make a pick");[m
[31m-                                    else[m
[32m+[m[32m                                    if (availablePicks.length > 0) {[m
[32m+[m[32m                                        if (side == "light") {[m
[32m+[m[32m                                            side = "Light";[m
[32m+[m[32m                                        }[m
[32m+[m[32m                                        if (side == "dark") {[m
[32m+[m[32m                                            side = "Dark";[m
[32m+[m[32m                                        }[m
[32m+[m[32m                                        that.messageDiv.text("Make a pick (stage " + stage + " / " + stages + " - " + side + ")");[m
[32m+[m[32m                                    }[m
[32m+[m[32m                                    else {[m
                                         that.messageDiv.text("Draft is finished");[m
[32m+[m[32m                                    }[m
                                 }[m
                             });[m
                             $(".card", that.picksDiv).remove();[m
[1mdiff --git a/gemp-swccg-async/src/main/web/leagueAdmin.html b/gemp-swccg-async/src/main/web/leagueAdmin.html[m
[1mindex 2d3ce362..72e9bb39 100644[m
[1m--- a/gemp-swccg-async/src/main/web/leagueAdmin.html[m
[1m+++ b/gemp-swccg-async/src/main/web/leagueAdmin.html[m
[36m@@ -235,8 +235,6 @@[m
     Start (YYYYMMDD): <input type="text" name="start"><br/>[m
     Format:[m
     <select name="format">[m
[31m-        <option value="c6_draft">Cube v6 Draft</option>[m
[31m-        <option value="c6obj_draft">Cube v6 Draft + Obj</option>[m
         <option value="c7_draft">Cube v7 Draft</option>[m
         <option value="c7obj_draft">Cube v7 Draft + Obj</option>[m
     </select><br/>[m
[1mdiff --git a/gemp-swccg-common/src/main/resources/gemp-swccg.properties b/gemp-swccg-common/src/main/resources/gemp-swccg.properties[m
[1mindex 049c3a93..36ed7ecb 100644[m
[1m--- a/gemp-swccg-common/src/main/resources/gemp-swccg.properties[m
[1m+++ b/gemp-swccg-common/src/main/resources/gemp-swccg.properties[m
[36m@@ -5,10 +5,10 @@[m [mapplication.root=/etc/gemp-swccg[m
 ## DB connection[m
 db.connection.class=org.gjt.mm.mysql.Driver[m
 db.connection.url=jdbc:mysql://localhost/gemp-swccg[m
[31m-db.connection.username=gemp-lotr[m
[31m-db.connection.password=gemp-lotr[m
[32m+[m[32mdb.connection.username=gemp[m
[32m+[m[32mdb.connection.password=gemp[m
 db.connection.validateQuery=/* ping */ select 1[m
 [m
[31m-port=8081[m
[32m+[m[32mport=8080[m
 #web.path=/env/gemp-swccg-dev/webdev/[m
 web.path=/env/gemp-swccg/web/[m
[1mdiff --git a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/DefaultSoloDraft.java b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/DefaultSoloDraft.java[m
[1mindex cb916c6b..49d24a5d 100644[m
[1m--- a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/DefaultSoloDraft.java[m
[1m+++ b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/DefaultSoloDraft.java[m
[36m@@ -86,6 +86,20 @@[m [mpublic class DefaultSoloDraft implements SoloDraft {[m
         return stage + 1 < _draftChoiceDefinitions.size();[m
     }[m
 [m
[32m+[m[32m    @Override[m
[32m+[m[32m    public int stageCount() {[m
[32m+[m[32m        return _draftChoiceDefinitions.size();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    @Override[m
[32m+[m[32m    public String draftSide(int stage) {[m
[32m+[m[32m        if (stage < _choiceCountPerSide) {[m
[32m+[m[32m            return "light";[m
[32m+[m[32m        } else {[m
[32m+[m[32m            return "dark";[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m
     @Override[m
     public int fixedCardCount() {[m
         return _fixedCardCount;[m
[1mdiff --git a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/SoloDraft.java b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/SoloDraft.java[m
[1mindex ef79b901..a6cc3c12 100644[m
[1m--- a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/SoloDraft.java[m
[1m+++ b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/SoloDraft.java[m
[36m@@ -11,6 +11,10 @@[m [mpublic interface SoloDraft {[m
 [m
     boolean hasNextStage(long seed, int stage);[m
 [m
[32m+[m[32m    int stageCount();[m
[32m+[m
[32m+[m[32m    String draftSide(int stage);[m
[32m+[m
     int fixedCardCount();[m
 [m
     int currentStage(CardCollection currentCards);[m
[36m@@ -21,5 +25,6 @@[m [mpublic interface SoloDraft {[m
         String getChoiceId();[m
         String getBlueprintId();[m
         String getChoiceUrl();[m
[32m+[m[32m        String getObjPackDescription();[m
     }[m
 }[m
[1mdiff --git a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/builder/DraftChoiceBuilder.java b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/builder/DraftChoiceBuilder.java[m
[1mindex 1bf98c31..ff9b48a2 100644[m
[1m--- a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/builder/DraftChoiceBuilder.java[m
[1m+++ b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/draft2/builder/DraftChoiceBuilder.java[m
[36m@@ -99,6 +99,9 @@[m [mpublic class DraftChoiceBuilder {[m
                                 public String getChoiceUrl() {[m
                                     return null;[m
                                 }[m
[32m+[m
[32m+[m[32m                                @Override[m
[32m+[m[32m                                public String getObjPackDescription() { return null; }[m
                             });[m
                 }[m
                 return draftChoices;[m
[36m@@ -153,6 +156,9 @@[m [mpublic class DraftChoiceBuilder {[m
                         public String getChoiceUrl() {[m
                             return url;[m
                         }[m
[32m+[m
[32m+[m[32m                        @Override[m
[32m+[m[32m                        public String getObjPackDescription() { return null; }[m
                     });[m
             cardsMap.put(choiceId, cardIds);[m
         }[m
[36m@@ -218,6 +224,9 @@[m [mpublic class DraftChoiceBuilder {[m
                                 public String getChoiceUrl() {[m
                                     return null;[m
                                 }[m
[32m+[m
[32m+[m[32m                                @Override[m
[32m+[m[32m                                public String getObjPackDescription() { return null; }[m
                             });[m
                 }[m
                 return draftableCards;[m
[36m@@ -293,6 +302,9 @@[m [mpublic class DraftChoiceBuilder {[m
                                 public String getChoiceUrl() {[m
                                     return null;[m
                                 }[m
[32m+[m
[32m+[m[32m                                @Override[m
[32m+[m[32m                                public String getObjPackDescription() { return null; }[m
                             });[m
                 }[m
                 return draftableCards;[m
[36m@@ -352,6 +364,21 @@[m [mpublic class DraftChoiceBuilder {[m
                 return result;[m
             }[m
 [m
[32m+[m[32m            String cardListForChoiceId(String choiceId) {[m
[32m+[m[32m                String result = "";[m
[32m+[m
[32m+[m[32m                for (ObjPack objPack : objPacks) {[m
[32m+[m[32m                    if (objPack.firstCardId.equals(choiceId)) {[m
[32m+[m[32m                        for (String cardId : objPack.cards) {[m
[32m+[m[32m                            result += cardId + ",";[m
[32m+[m[32m                        }[m
[32m+[m[32m                        result = result.substring(0, result.length()-1);[m
[32m+[m[32m                        break;[m
[32m+[m[32m                    }[m
[32m+[m[32m                }[m
[32m+[m[32m                return result;[m
[32m+[m[32m            }[m
[32m+[m
             @Override[m
             public Iterable<SoloDraft.DraftChoice> getDraftChoice(long seed, int stage, CardCollection currentCards, String currentChoice) {[m
                 List<String> tempCards = getShuffledCardsSpecial(seed, stage);[m
[36m@@ -388,6 +415,9 @@[m [mpublic class DraftChoiceBuilder {[m
                                 public String getChoiceUrl() {[m
                                     return null;[m
                                 }[m
[32m+[m
[32m+[m[32m                                @Override[m
[32m+[m[32m                                public String getObjPackDescription() { return cardListForChoiceId(getChoiceId()); }[m
                             });[m
                 }[m
                 return draftableCards;[m
[1mdiff --git a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/league/SoloDraftLeagueData.java b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/league/SoloDraftLeagueData.java[m
[1mindex e18ee916..14a869fc 100644[m
[1m--- a/gemp-swccg-server/src/main/java/com/gempukku/swccgo/league/SoloDraftLeagueData.java[m
[1m+++ b/gemp-swccg-server/src/main/java/com/gempukku/swccgo/league/SoloDraftLeagueData.java[m
[36m@@ -86,6 +86,8 @@[m [mpublic class SoloDraftLeagueData implements LeagueData {[m
             stage -= _draft.fixedCardCount();[m
             extraInformation.put("finished", (_draft.hasNextStage(seed, stage) == false));[m
             extraInformation.put("stage", stage);[m
[32m+[m[32m            extraInformation.put("draftSide", _draft.draftSide(stage));[m
[32m+[m[32m            extraInformation.put("stageCount", _draft.stageCount());[m
             extraInformation.put("seed", seed);[m
             collection.setExtraInformation(extraInformation);[m
         }[m
[36m@@ -95,6 +97,8 @@[m [mpublic class SoloDraftLeagueData implements LeagueData {[m
         Map<String, Object> extraInformation = new HashMap<String, Object>();[m
         extraInformation.put("finished", false);[m
         extraInformation.put("stage", 0);[m
[32m+[m[32m        extraInformation.put("draftSide", "light");[m
[32m+[m[32m        extraInformation.put("stageCount", _draft.stageCount());[m
         extraInformation.put("seed", seed);[m
         return extraInformation;[m
     }[m
[1mdiff --git a/gemp-swccg-server/src/main/resources/swccgDrafts.json b/gemp-swccg-server/src/main/resources/swccgDrafts.json[m
[1mindex 625053bb..b6ec46e2 100644[m
[1m--- a/gemp-swccg-server/src/main/resources/swccgDrafts.json[m
[1m+++ b/gemp-swccg-server/src/main/resources/swccgDrafts.json[m
[36m@@ -1,12 +1,4 @@[m
 [[m
[31m-  {[m
[31m-    "type": "c6_draft",[m
[31m-    "location": "/draft/cube6Draft.json"[m
[31m-  },[m
[31m-  {[m
[31m-    "type": "c6obj_draft",[m
[31m-    "location": "/draft/cubeObj6Draft.json"[m
[31m-  },[m
   {[m
     "type": "c7_draft",[m
     "location": "/draft/cube7Draft.json"[m
