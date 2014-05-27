package com.tinkerpop.gremlin.giraph.process.computer;

import com.tinkerpop.gremlin.giraph.process.computer.util.ConfUtil;
import com.tinkerpop.gremlin.giraph.structure.GiraphGraph;
import com.tinkerpop.gremlin.giraph.structure.GiraphVertex;
import com.tinkerpop.gremlin.giraph.structure.io.EmptyOutEdges;
import com.tinkerpop.gremlin.process.computer.GraphComputer;
import com.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.giraph.conf.GiraphConfiguration;
import org.apache.giraph.job.GiraphJob;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;

import java.io.File;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GiraphGraphRunner extends Configured implements Tool {

    private final GiraphConfiguration giraphConfiguration;

    public GiraphGraphRunner(final org.apache.hadoop.conf.Configuration hadoopConfiguration) {
        this.giraphConfiguration = new GiraphConfiguration();
        hadoopConfiguration.forEach(entry -> this.giraphConfiguration.set(entry.getKey(), entry.getValue()));
        this.giraphConfiguration.setMasterComputeClass(GiraphGraphComputerSideEffects.class);
        this.giraphConfiguration.setVertexClass(GiraphVertex.class);
        this.giraphConfiguration.setOutEdgesClass(EmptyOutEdges.class);
        this.giraphConfiguration.setClass("giraph.vertexIdClass", LongWritable.class, LongWritable.class);
        this.giraphConfiguration.setClass("giraph.vertexValueClass", Text.class, Text.class);
    }

    public int run(final String[] args) {
        try {
            final GiraphJob job = new GiraphJob(this.giraphConfiguration,
                    "GiraphGremlin: " + VertexProgram.createVertexProgram(ConfUtil.apacheConfiguration(this.giraphConfiguration)));
            //job.getInternalJob().setJarByClass(GiraphGraphComputer.class);
            FileInputFormat.addInputPath(job.getInternalJob(), new Path(this.giraphConfiguration.get(GiraphGraphComputer.GREMLIN_INPUT_LOCATION)));
            FileOutputFormat.setOutputPath(job.getInternalJob(), new Path(this.giraphConfiguration.get(GiraphGraphComputer.GREMLIN_OUTPUT_LOCATION)));
            job.run(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
        return 0;
    }

    public static void main(final String[] args) throws Exception {
        try {
            final FileConfiguration configuration = new PropertiesConfiguration();
            configuration.load(new File(args[0]));
            GraphComputer computer = new GiraphGraphComputer(GiraphGraph.open(), configuration);
            computer.program(configuration).submit().get();
        } catch (Exception e) {
            System.out.println(e);
            throw e;
        }
    }
}