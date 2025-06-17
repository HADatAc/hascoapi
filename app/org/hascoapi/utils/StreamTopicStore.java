package org.hascoapi.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hascoapi.entity.pojo.StreamTopic;

public class StreamTopicStore {

    private static StreamTopicStore store = null;

    // public variables
    final private Map<String,StreamTopic> cache;

    private StreamTopicStore() {
        cache = new HashMap<String,StreamTopic>();
        refreshStore();
    }

    // static method to create instance of Singleton class
    public static StreamTopicStore getInstance()
    {
        if (store == null)
            store = new StreamTopicStore();

        return store;
    }

    public StreamTopic findCachedByUri(String streamTopicUri) {
        return cache.get(streamTopicUri);
    }

    public List<StreamTopic> findCachedOpenStreams() {
        List<StreamTopic> list = new ArrayList<StreamTopic>();
        for (Map.Entry<String, StreamTopic> entry: cache.entrySet()) {
            list.add(entry.getValue());
        }
        Collections.sort(list);
        return list;
    }

    public void refreshStore() {
        this.cache.clear();
        List<StreamTopic> cacheList = StreamTopic.findOpenStreamTopics();
        if (cacheList == null) {
            return;
        }
        for (StreamTopic stream : cacheList) {
            this.cache.put(stream.getUri(), stream);
        }
    }

}
