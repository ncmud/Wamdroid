package org.ncmud.mudwammer.responder;

public interface TriggerResponderEditorDoneListener {

    void newTriggerResponder(TriggerResponder newresponder);

    void editTriggerResponder(TriggerResponder edited, TriggerResponder original);
}
