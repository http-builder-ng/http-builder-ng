/*
 * Copyright (C) 2017 HttpBuilder-NG Project
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
package groovyx.net.http

import groovyx.net.http.MultipartContent.MultipartPart
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Path
import java.util.function.Consumer

class MultipartContentSpec extends Specification {

    @Rule TemporaryFolder folder = new TemporaryFolder()

    def 'groovy dsl'() {
        setup:
        File echoFile = folder.newFile('echo.txt')
        echoFile.text = 'Again, this is text.'

        when:
        MultipartContent multipart = MultipartContent.multipart {
            field 'alpha', 'bravo'
            part 'charlie', 'charlie.txt', 'text/plain', 'This is some text'.bytes
            part 'delta', 'delta.txt', 'text/plain', 'This is also text'
            part 'echo', echoFile.name, 'text/plain', echoFile.toPath()
            part 'foxtrot', echoFile.name, 'text/plain', echoFile.newInputStream()
        }

        then:
        assertField multipart.parts()[0], 'alpha', 'bravo'
        assertFile multipart.parts()[1], 'charlie', 'charlie.txt', 'text/plain', isBytes('This is some text')
        assertFile multipart.parts()[2], 'delta', 'delta.txt', 'text/plain', isString('This is also text')
        assertFile multipart.parts()[3], 'echo', 'echo.txt', 'text/plain', isPath(echoFile.toPath())
        assertFile multipart.parts()[4], 'foxtrot', 'echo.txt', 'text/plain', isStream(echoFile)
    }

    def 'java function'() {
        setup:
        File echoFile = folder.newFile('echo.txt')
        echoFile.text = 'Again, this is text.'

        when:
        MultipartContent multipart = MultipartContent.multipart(new Consumer<MultipartContent>() {
            @Override void accept(final MultipartContent mc) {
                mc.field 'alpha', 'bravo'
                mc.part 'charlie', 'charlie.txt', 'text/plain', 'This is some text'.bytes
                mc.part 'delta', 'delta.txt', 'text/plain', 'This is also text'
                mc.part 'echo', echoFile.name, 'text/plain', echoFile.toPath()
                mc.part 'foxtrot', echoFile.name, 'text/plain', echoFile.newInputStream()
            }
        })

        then:
        assertField multipart.parts()[0], 'alpha', 'bravo'
        assertFile multipart.parts()[1], 'charlie', 'charlie.txt', 'text/plain', isBytes('This is some text')
        assertFile multipart.parts()[2], 'delta', 'delta.txt', 'text/plain', isString('This is also text')
        assertFile multipart.parts()[3], 'echo', 'echo.txt', 'text/plain', isPath(echoFile.toPath())
        assertFile multipart.parts()[4], 'foxtrot', 'echo.txt', 'text/plain', isStream(echoFile)
    }

    private static Closure<Boolean> isBytes(final String value) {
        return { c -> c instanceof byte[] && new String(c) == value }
    }

    private static Closure<Boolean> isString(final String value) {
        return { c -> c instanceof String && c == value }
    }

    private static Closure<Boolean> isPath(final Path value) {
        return { c -> c instanceof Path && c == value }
    }

    private static Closure<Boolean> isStream(final File file) {
        return { c -> c instanceof InputStream && c.text == file.text }
    }

    private static boolean assertField(MultipartPart entry, String name, String value) {
        assert entry.fieldName == name
        assert entry.content == value
        assert entry.contentType == 'text/plain'
        assert !entry.fileName
        true
    }

    private static boolean assertFile(MultipartPart entry, String fieldName, String fileName, String type, Closure<Boolean> check) {
        assert entry.fieldName == fieldName
        assert entry.fileName == fileName
        assert entry.contentType == type
        assert check.call(entry.content)
        true
    }
}
