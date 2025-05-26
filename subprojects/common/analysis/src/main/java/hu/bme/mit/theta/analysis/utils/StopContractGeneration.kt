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

package hu.bme.mit.theta.analysis.utils

import hu.bme.mit.theta.analysis.Prec

object StopContractGeneration {
    lateinit var criterion: (Prec) -> Boolean
    private var stop = false
    private var unchangedPrecCount = 0
    private var previousSize = 0

    fun <P: Prec> shouldStop(prec: P): Boolean {
        if (!stop)
            stop = criterion(prec)
        return stop
    }

    fun precisionStuck(newPrecSize: Int): Boolean {
        if (newPrecSize > previousSize) unchangedPrecCount = 0
        else unchangedPrecCount++
        previousSize = newPrecSize
        return unchangedPrecCount >= 10
    }
}