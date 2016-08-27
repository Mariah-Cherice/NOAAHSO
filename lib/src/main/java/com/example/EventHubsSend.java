package com.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.*;
import java.util.*;


import java.util.concurrent.ExecutionException;
import com.microsoft.azure.eventhubs.*;
import com.microsoft.azure.servicebus.*;


import org.apache.http.Consts;

public class EventHubsSend {




    public static void main(String[] args)
            throws ServiceBusException, ExecutionException, InterruptedException, IOException
    {
        //System.out.println("Hello World");
        final String namespaceName = "weatherpractice-ns";
        final String eventHubName = "weatherpractice";
        final String sasKeyName = "SendRule";
        final String sasKey = "od7d2AOLfOWvT+wc/KReiBGlDexBgGHHZ/3GuklpQXA=";
        ConnectionStringBuilder connStr = new ConnectionStringBuilder(namespaceName, eventHubName, sasKeyName, sasKey);
        //Endpoint=sb://weatherpractice-ns.servicebus.windows.net/;SharedAccessKeyName=SendRule;SharedAccessKey=od7d2AOLfOWvT+wc/KReiBGlDexBgGHHZ/3GuklpQXA=;EntityPath=weatherpractice


        byte[] payloadBytes = "Here is some data".getBytes("UTF-8");
        EventData sendEvent = new EventData(payloadBytes);

        EventHubClient ehClient = EventHubClient.createFromConnectionStringSync(connStr.toString());
        ehClient.sendSync(sendEvent);

    }

}
