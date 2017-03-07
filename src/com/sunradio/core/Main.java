package com.sunradio.core;

import com.external.WavFile;
import com.sunradio.math.DFTStraight;
import com.sunradio.math.Filter;
import com.sunradio.math.DFTInverse;

import java.io.File;

/**
 * @author V.Kremneva
 */
public class Main {

    /**
     *  Move data to the left with filling with 0
     * @param data data to move
     * @param offset amount of steps to move
     * @return array with nulls in the end and 'data' values moved on offset
     */
    public static double[] move(double[] data, int offset) {
        double[] result = new double[data.length];

        System.arraycopy(data, offset, result, 0, data.length - offset);

        return result;
    }

    public static void main(String[] args) {
        try {
            WavFile wavInput = WavFile.openWavFile(new File("C:\\Users\\Merveilleuse\\IdeaProjects\\SunRadio\\launch.wav"));

            WavFile wavOutput = WavFile.newWavFile(new File("C:\\Users\\Merveilleuse\\IdeaProjects\\SunRadio\\new1.wav"),
                    wavInput.getNumChannels(), wavInput.getNumFrames(),
                    wavInput.getValidBits(), wavInput.getSampleRate());

            final int FRAMES = 2048;
            final int OVERLAP = 16;
            int numChannels = wavInput.getNumChannels();
            int bufferIndAmount = FRAMES * numChannels;
            int overlapIndAmount = OVERLAP * numChannels;
            int offset = FRAMES / OVERLAP;
            int outputBufferIndAmount = bufferIndAmount + overlapIndAmount;
            long wholeIndAmount = wavInput.getNumFrames() * numChannels;
            double[] buffer = new double[bufferIndAmount];
            double[] outputBuffer = new double[outputBufferIndAmount];
            double[] modulated, outputWindowFunction, prevPhases, currentPhases;
            double lightLevel;

            int frames_read;
            DFTStraight transformable;
            transformable = new DFTStraight();
            prevPhases = new double[bufferIndAmount];
            do {
                //read next 'FRAMES' into buffer -- amplitudes(t)
                frames_read = wavInput.readFramesWithOverlap(buffer, FRAMES, OVERLAP);

                //get current level of light
                lightLevel = LightLevel.getAverageLightLevel(buffer);

                for (int i = 0; i < overlapIndAmount; i++) {
                    //todo: test edges
                    //apply window filter. first and last 'offset' goes without filter
                    if ((wavInput.getFrameCounter() > offset) ||
                            (wavInput.getFrameCounter() < wholeIndAmount - offset)) {
                        buffer = Filter.apply(buffer, Filter.BlackmanNuttall(bufferIndAmount));
                    }

                    //run Fourier transform
                    transformable.run(buffer);

                    //todo: tone modulation

                    //correct amplitudes(t) according to the light level
                    //buffer = AM.modulate(buffer, lightLevel);

                    //run inverse Fourier transform
                    buffer = DFTInverse.run(transformable.getData());

                    //apply output window function
                    outputWindowFunction = Filter.getOutputWindowFunc(Filter.BlackmanNuttall(bufferIndAmount));
                    buffer = Filter.apply(buffer, outputWindowFunction);

                    for (int j = 0; j < bufferIndAmount; j++)
                            outputBuffer[j + i] += buffer[j]; // todo: "+"?

                    //read next 'FRAMES' into buffer -- amplitudes(t)
                    frames_read = wavInput.readFramesWithOverlap(buffer, FRAMES, OVERLAP);
                }

                //write data to new .waw file
                wavOutput.writeFrames(outputBuffer, FRAMES);

                //move data for overlap
                outputBuffer = move(outputBuffer, overlapIndAmount);

            } while (frames_read != 0);

            wavInput.close(); wavOutput.close();

        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
