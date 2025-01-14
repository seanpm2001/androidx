/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.solver.query.result

import androidx.room.compiler.processing.isArray
import androidx.room.ext.L
import androidx.room.ext.T
import androidx.room.solver.CodeGenScope
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName

class ArrayQueryResultAdapter(
    private val rowAdapter: RowAdapter
) : QueryResultAdapter(listOf(rowAdapter)) {
    val type = rowAdapter.out
    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder().apply {
            rowAdapter.onCursorReady(cursorVarName = cursorVarName, scope = scope)

            val arrayType = ArrayTypeName.of(type.typeName)

            if (type.isArray()) {
                addStatement(
                    "final $T $L = new $T[$L.getCount()][]",
                    arrayType, outVarName, type.componentType.typeName, cursorVarName
                )
            } else {
                addStatement(
                    "final $T $L = new $T[$L.getCount()]",
                    arrayType, outVarName, type.typeName, cursorVarName
                )
            }

            val tmpVarName = scope.getTmpVar("_item")
            val indexVar = scope.getTmpVar("_index")
            addStatement("$T $L = 0", TypeName.INT, indexVar)
            beginControlFlow("while($L.moveToNext())", cursorVarName).apply {
                addStatement("final $T $L", type.typeName, tmpVarName)
                rowAdapter.convert(tmpVarName, cursorVarName, scope)
                addStatement("$L[$L] = $L", outVarName, indexVar, tmpVarName)
                addStatement("$L ++", indexVar)
            }
            endControlFlow()
        }
    }
}
