/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.registry.mongodb.converters;

import java.net.URI;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.gravitee.model.Api;
import io.gravitee.model.ApiState;

/**
 * A MongoDB converter to convert {@code Api} and {@code DBObject}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class ApiConverter extends AbstractConverter<Api, DBObject> {

    @Override
    public DBObject convertTo(final Api api) {
        if (api == null) {
            return null;
        }
        final DBObject dbObject = new BasicDBObject();
        if (api.getName() != null && !api.getName().isEmpty()) {
            dbObject.put("name", api.getName());
        }
        if (api.getVersion() != null && !api.getVersion().isEmpty()) {
            dbObject.put("version", api.getVersion());
        }
        if (api.getState() != null) {
            dbObject.put("state", api.getState().name());
        }
        if (api.getTargetURI() != null) {
            dbObject.put("target", api.getTargetURI().getPath());
        }
        if (api.getPublicURI() != null) {
            dbObject.put("public", api.getPublicURI().getPath());
        }
        return dbObject;
    }

    @Override
    public Api convertFrom(final DBObject dbobject) {
        if (dbobject == null) {
            return null;
        }
        final Api api = new Api();
        final Object name = dbobject.get("name");
        if (name != null) {
            api.setName((String) name);
        }
        final Object version = dbobject.get("version");
        if (version != null) {
            api.setVersion((String) version);
        }
        final Object publicURI = dbobject.get("public");
        if (publicURI != null) {
            api.setPublicURI(URI.create((String) publicURI));
        }
        final Object state = dbobject.get("state");
        if (state != null) {
            api.setState(ApiState.valueOf((String) state));
        }
        final Object target = dbobject.get("target");
        if (target != null) {
            api.setTargetURI(URI.create((String) target));
        }
        return api;
    }
}
