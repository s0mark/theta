/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.frontend.transformation.model.types.simple;

import static com.google.common.base.Preconditions.checkNotNull;

import hu.bme.mit.theta.common.Tuple2;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.frontend.ParseContext;
import hu.bme.mit.theta.frontend.transformation.model.declaration.CDeclaration;
import hu.bme.mit.theta.frontend.transformation.model.types.complex.CComplexType;
import hu.bme.mit.theta.frontend.transformation.model.types.complex.compound.CPointer;
import hu.bme.mit.theta.frontend.transformation.model.types.complex.compound.CStruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Struct extends NamedType {
    public static final String UNION_LAST_WRITTEN = "union_last_written";

    private final Map<String, CDeclaration> fields;
    private final String name;
    public final boolean isUnion;
    private final Logger uniqueWarningLogger;

    private boolean currentlyBeingBuilt;
    private static final Map<String, Struct> definedTypes = new LinkedHashMap<>();

    public static Struct getByName(String name) {
        return definedTypes.get(name);
    }

    Struct(String name, ParseContext parseContext, Logger uniqueWarningLogger, boolean isUnion) {
        super(parseContext, "struct", uniqueWarningLogger);
        this.uniqueWarningLogger = uniqueWarningLogger;
        fields = new LinkedHashMap<>();
        this.name = name;
        if (name != null) {
            definedTypes.put(name, this);
        }
        this.isUnion = isUnion;
        if (this.isUnion) {
            CSimpleType type = CSimpleTypeFactory.NamedType("int", parseContext, uniqueWarningLogger);
            type.setSigned(false);
            type.setAssociatedName(UNION_LAST_WRITTEN);
            fields.put(UNION_LAST_WRITTEN, new CDeclaration(type));
        }
        currentlyBeingBuilt = false;
    }

    private Struct(Struct from) {
        super(from.parseContext, "struct", from.uniqueWarningLogger);
        fields = new LinkedHashMap<>();
        fields.putAll(from.fields);
        this.name = from.name;
        this.isUnion = from.isUnion;
        this.uniqueWarningLogger = from.uniqueWarningLogger;
        currentlyBeingBuilt = false;
    }

    public void addField(CDeclaration decl) {
        fields.put(checkNotNull(decl.getName()), checkNotNull(decl));
    }

    @Override
    public CComplexType getActualType() {
        if (currentlyBeingBuilt) {
            uniqueWarningLogger.write(
                    Level.INFO, "WARNING: self-embedded structs! Using long as a placeholder\n");
            return CComplexType.getSignedInt(parseContext);
        }
        currentlyBeingBuilt = true;
        List<Tuple2<String, CComplexType>> actualFields = new ArrayList<>();
        fields.forEach(
                (s, cDeclaration) -> actualFields.add(Tuple2.of(s, cDeclaration.getActualType())));
        currentlyBeingBuilt = false;

        CComplexType type = new CStruct(this, actualFields, parseContext, isUnion);

        for (int i = 0; i < getPointerLevel(); i++) {
            type = new CPointer(this, type, parseContext);
        }

        return type;
    }

    @Override
    public CSimpleType getBaseType() {
        return this;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public CSimpleType copyOf() {
        var ret = new Struct(this);
        setUpCopy(ret);
        return ret;
    }
}
