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

package cn.ponfee.disjob.supervisor.dag;

import cn.ponfee.disjob.common.util.Files;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * DAG printer
 *
 * @author Ponfee
 */
public class DAGPrinter {

    public static void main(String[] args) throws Exception {
        drawGraph("A", "dag01.png");
        drawGraph("A -> B,C,D", "dag02.png");
        drawGraph("A,B,C -> D", "dag03.png");
        drawGraph("A -> B,C,D -> E", "dag04.png");
        drawGraph("A -> B,C -> E,(F->G) -> H", "dag05.png");
        drawGraph("A -> (B->C->D),(A->F) -> G,H,X -> J ; A->Y", "dag06.png");
        drawGraph("ALoader -> (BMap->CMap->DMap),(AMap->FMap) -> GShuffle,HShuffle,XShuffle -> JReduce ; A->Y", "dag07.png");
        drawGraph("A->B,C,(D->E)->D,F->G", "dag08.png");
        drawGraph("[\"A->C\",\"A->D\",\"B->D\",\"B->E\"]", "dag09.png");
        drawGraph("[\"1:1:A->1:1:C\", \"1:1:A->1:1:D\", \"1:1:B->1:1:D\", \"1:1:B->1:1:E\"]", "dag10.png");
        drawGraph("A->B,C,D",                 "dag11.png");
        drawGraph("A->B->C,D",                "dag12.png");
        drawGraph("A->B->C->D->G;A->E->F->G", "dag13.png");
        drawGraph("A->(B->C->D),(E->F)->G",   "dag14.png");
        drawGraph("A->B->C,D,E;A->H->I,J,K",  "dag15.png");
        drawGraph("A->(B->C,D,E),(H->I,J,K)", "dag16.png");
        drawGraph("A,B,C->D",                 "dag17.png");

        drawGraph("A -> B -> (D->E->F), ( C -> (G -> (H->I),J -> K), (L->M) ) -> Z", "dag20.png");
        drawGraph("[\"A -> B\",\"B -> D\",\"D -> E\",\"E -> F\",\"F -> Z\",\"B -> C\",\"C -> G\",\"G -> H\",\"H -> I\",\"I -> K\",\"K -> Z\",\"G -> J\",\"J -> K\",\"C -> L\",\"L -> M\",\"M -> Z\"]", "dag21.png");
        drawGraph("A -> (B->C), (D->E->F), ( G -> (H->I->J,K),(L->M) -> N ) -> Z", "dag22.png");
        drawGraph("[\"A -> B\",\"B -> C\",\"C -> Z\",\"A -> D\",\"D -> E\",\"E -> F\",\"F -> Z\",\"A -> G\",\"G -> H\",\"H -> I\",\"I -> J\",\"J -> N\",\"I -> K\",\"K -> N\",\"G -> L\",\"L -> M\",\"M -> N\",\"N -> Z\"]", "dag23.png");
    }

    private static void drawGraph(String expr, String fileName) throws IOException {
        File file = new File(MavenProjects.getProjectBaseDir() + "/target/dag/" + fileName);
        FileUtils.deleteQuietly(file);
        Files.mkdirIfNotExists(file.getParentFile());
        DAGUtils.drawImage(expr, false, 2000, new FileOutputStream(file));
    }

}
