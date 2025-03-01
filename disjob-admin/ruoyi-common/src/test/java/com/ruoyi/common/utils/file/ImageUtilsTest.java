/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruoyi.common.utils.file;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

/**
 * ImageUtils Test
 *
 * @author Ponfee
 */
public class ImageUtilsTest {

    public static void main(String[] args) throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String path = new File(url.toURI()).getParentFile().getParentFile().getAbsolutePath() + "/target/dag/test.png";
        File file = new File(path);
        FileUtils.deleteQuietly(file);
        file.getParentFile().mkdirs();

        String text = "[Error]: fdsafsdafs,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsdas,fdsafdsafsadfsadfsda";
        ImageUtils.createImage(text, new FileOutputStream(file), 1000, 600);
    }
}
