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

import cn.ponfee.disjob.common.dag.DAGExpression;
import cn.ponfee.disjob.common.util.MavenProjects;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * DAG printer test
 *
 * @author Ponfee
 */
@Disabled
class DAGPrinterTest {

    private static final String BASE_DIR = MavenProjects.getProjectBaseDir() + "/target/dag/";

    @Test
    void testPrint() throws Exception {
        File baseDir = new File(BASE_DIR);
        FileUtils.deleteDirectory(baseDir);
        FileUtils.forceMkdir(baseDir);

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

        String dag20 = "A -> B -> (D->E->F), ( C -> (G -> (H->I),J -> K), (L->M) ) -> Z";
        String dag21 = "[\"A -> B\",\"B -> D\",\"D -> E\",\"E -> F\",\"F -> Z\",\"B -> C\",\"C -> G\",\"G -> H\",\"H -> I\",\"I -> K\",\"K -> Z\",\"G -> J\",\"J -> K\",\"C -> L\",\"L -> M\",\"M -> Z\"]";
        Assertions.assertEquals(DAGExpression.parse(dag20), DAGExpression.parse(dag21));
        drawGraph(dag20, "dag20.png");
        drawGraph(dag21, "dag21.png");

        String dag30 = "A -> (B->C), (D->E->F), ( G -> (H->I->J,K),(L->M) -> N ) -> Z";
        String dag31 = "[\"A -> B\",\"B -> C\",\"C -> Z\",\"A -> D\",\"D -> E\",\"E -> F\",\"F -> Z\",\"A -> G\",\"G -> H\",\"H -> I\",\"I -> J\",\"J -> N\",\"I -> K\",\"K -> N\",\"G -> L\",\"L -> M\",\"M -> N\",\"N -> Z\"]";
        Assertions.assertEquals(DAGExpression.parse(dag30), DAGExpression.parse(dag31));
        drawGraph(dag30, "dag30.png");
        drawGraph(dag31, "dag31.png");
    }

    private static void drawGraph(String expr, String fileName) throws IOException {
        DAGUtils.drawImage(expr, false, 2000, new FileOutputStream(BASE_DIR + fileName));
    }

}
