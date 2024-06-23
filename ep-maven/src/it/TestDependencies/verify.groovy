/*
 * Copyright (C) 2021-2024 Cloud Software Group, Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path
import java.util.zip.ZipFile

assert new File(basedir, "build.log")
        .text.contains("TestDependencies ................................... SUCCESS"):
        "Text with SUCCESS not found"

def extractFromZip(File zFile, String toExtract) {
    def zipFile = new ZipFile(zFile)
    Path extracted = basedir.toPath().resolve(toExtract + ".extracted");
    zipFile.entries().each { it ->

        if (!it.getName().contains(toExtract)) {
            return;
        }

        Files.copy(zipFile.getInputStream(it), extracted)
    }

    return extracted.toFile();
}

File extracted = extractFromZip(basedir.toPath().resolve(Paths.get("B", "target", "TestDependencies_B-" + CURRENT_PROJECT_VERSION + "-ep-eventflow-fragment.zip")).toFile(),
        "MANIFEST")

assert extracted.text.contains("TIBCO-EP-Event-Modules: B.B3"): "B.B3 not found";
assert !extracted.text.contains("TIBCO-EP-Event-Modules: B.B1"): "B.B1 was found";
assert new File(basedir, "B/target/test-classes/com/streambase/generated/providers/B/B1_ModuleSourceProvider.class")
        .exists(): "Could not find B/target/test-classes/com/streambase/generated/providers/B/B1_ModuleSourceProvider.class";