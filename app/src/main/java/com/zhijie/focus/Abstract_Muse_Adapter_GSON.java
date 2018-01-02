package com.zhijie.focus;

import com.google.gson.*;
import java.lang.reflect.Type;
import com.choosemuse.libmuse.Muse;

public class Abstract_Muse_Adapter_GSON  implements JsonSerializer<Muse>, JsonDeserializer<Muse> {
    @Override
    public JsonElement serialize(Muse src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("type", new JsonPrimitive(src.getClass().getSimpleName()));
        result.add("properties", context.serialize(src, src.getClass()));
        return result;
    }


    @Override
    public Muse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");

        try {
            String thepackage = "com.choosemuse.libmuse.";
            return context.deserialize(element, Class.forName(thepackage + type));
        } catch (ClassNotFoundException cnfe) {
            throw new JsonParseException("Unknown element type: " + type, cnfe);
        }
    }
}