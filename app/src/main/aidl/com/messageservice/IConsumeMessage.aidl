// IConsumeMessage.aidl
package com.messageservice;

// Declare any non-default types here with import statements

interface IConsumeMessage {

void speakTextMessage(String message,in ResultReceiver receiver);

}
