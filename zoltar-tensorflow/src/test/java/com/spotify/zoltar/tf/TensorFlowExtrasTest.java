/*-
 * -\-\-
 * zoltar-tensorflow
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
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
 * -/-/-
 */

package com.spotify.zoltar.tf;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.tensorflow.*;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TensorFlowExtrasTest {

  private static final String mul2 = "mul2";
  private static final String mul3 = "mul3";

  private static Graph createDummyGraph() {
    Tensor<Double> t2 = Tensors.create(2.0);
    Tensor<Double> t3 = Tensors.create(3.0);

    Graph graph = new Graph();
    Output<Double> input = graph.opBuilder("Placeholder", "input")
            .setAttr("dtype", DataType.DOUBLE)
            .build().output(0);

    Output<Double> two = graph.opBuilder("Const", "two")
            .setAttr("dtype", t2.dataType())
            .setAttr("value", t2)
            .build().output(0);

    Output<Double> three = graph.opBuilder("Const", "three")
            .setAttr("dtype", t3.dataType())
            .setAttr("value", t3)
            .build().output(0);


    graph.opBuilder("Mul", mul2)
            .addInput(input)
            .addInput(two).build();

    graph.opBuilder("Mul", mul3)
            .addInput(input)
            .addInput(three).build();

    return graph;
  }

  @Test
  public void testExtract1() {
    Graph graph = createDummyGraph();
    Session session = new Session(graph);
    Session.Runner runner = session.runner();
    runner.feed("input", Tensors.create(10.0));
    Map<String, JTensor> result = TensorFlowExtras.runAndExtract(runner, mul2);
    assertEquals(Sets.newHashSet(mul2), result.keySet());
    assertScalar(result.get(mul2), 20.0);
    session.close();
    graph.close();
  }

  @Test
  public void testExtract2a() {
    Graph graph = createDummyGraph();
    Session session = new Session(graph);
    Session.Runner runner = session.runner();
    runner.feed("input", Tensors.create(10.0));
    Map<String, JTensor> result = TensorFlowExtras.runAndExtract(runner, mul2, mul3);
    assertEquals(Lists.newArrayList(mul2, mul3), new ArrayList<>(result.keySet()));
    assertScalar(result.get(mul2), 20.0);
    assertScalar(result.get(mul3), 30.0);
    session.close();
    graph.close();
  }

  @Test
  public void testExtract2b() {
    Graph graph = createDummyGraph();
    Session session = new Session(graph);
    Session.Runner runner = session.runner();
    runner.feed("input", Tensors.create(10.0));
    Map<String, JTensor> result = TensorFlowExtras.runAndExtract(runner, mul3, mul2);
    assertEquals(Lists.newArrayList(mul3, mul2), new ArrayList<>(result.keySet()));
    assertScalar(result.get(mul2), 20.0);
    assertScalar(result.get(mul3), 30.0);
    session.close();
    graph.close();
  }

  private void assertScalar(JTensor jt, Double value) {
    assertEquals(DataType.DOUBLE, jt.dataType());
    assertEquals(0, jt.numDimensions());
    assertEquals(0, jt.shape().length);
    double[] expected = {value};
    assertArrayEquals(expected, jt.doubleValue(), 0.0);
  }
}