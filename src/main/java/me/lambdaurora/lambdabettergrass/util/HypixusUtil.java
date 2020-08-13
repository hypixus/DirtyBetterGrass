package me.lambdaurora.lambdabettergrass.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class HypixusUtil {


    public enum dirtTexture {
        DEFAULT,
        GRASSY,
        SNOWY
    }

    final public static boolean DEBUG = false;


    // always remove 1 from z shown in mc
    final public static int VERBOSE_X = 3496;
    final public static int VERBOSE_Y = 250;
    final public static int VERBOSE_Z = -1111;

    final public static boolean[] falseQuad = {false, false, false, false};

    /**
     * Logs certain event under mod with prefix "[hypixus] ".
     *
     * @param message Message to be displayed in console.
     */
    public static void log(String message) {
        System.out.println("[hypixus] " + message);
    }

    /**
     * Checks whether given block is an instance of another block.
     *
     * @param blockState  Block to be checked.
     * @param inGameBlock In-game block to be compared against.
     * @return effect of the comparison.
     */
    public static boolean compareBlocks(@NotNull BlockState blockState, @NotNull Block inGameBlock) {
        return blockState == inGameBlock.getDefaultState();
    }

    /**
     * Checks whether given block is visible on all the sides original block is.
     *
     * @param original  Original block to be compared against.
     * @param toCompare Block to be compared.
     * @return True for visible, false for invisible.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isVisibleOnOriginalSides(boolean[] original, boolean[] toCompare) {
        for (int i = 0; i < 4; i++) {
            if (original[i] && !toCompare[i]) return false;
        }
        return true;
    }

    /**
     * Checks which sides of the block are unobstructed.
     *
     * @param world    RenderView used to get block states.
     * @param position Position of the block.
     * @return Array of four booleans, corresponding to north, south, west and east respectively.
     */
    public static boolean[] visibleSides(@NotNull BlockRenderView world, @NotNull BlockPos position, boolean verbose) {
        boolean[] toReturn = new boolean[4];
        BlockPos oneHigher = position.up();
        toReturn[0] = world.isSkyVisible(oneHigher.north());
        toReturn[1] = world.isSkyVisible(oneHigher.south());
        toReturn[2] = world.isSkyVisible(oneHigher.west());
        toReturn[3] = world.isSkyVisible(oneHigher.east());
        if (verbose) {
            log("north:" + (toReturn[0] ? "yes" : "no"));
            log("south:" + (toReturn[1] ? "yes" : "no"));
            log("west:" + (toReturn[2] ? "yes" : "no"));
            log("east:" + (toReturn[3] ? "yes" : "no"));
        }
        return toReturn;
    }

    //@TODO fix dirt not grassy under trees
    //@TODO optimizations

    /**
     * Analyzes whether given block of dirt should have grassy sides.
     *
     * @param world     RenderView used to get block positions.
     * @param basePos   Target block position. Used to scan area for ability of grass growth.
     * @param baseBlock Target block.
     */
    public static dirtTexture grassyDirt(@NotNull BlockRenderView world, @NotNull BlockPos basePos, @NotNull BlockState baseBlock) {
        return DEBUG ? grassDirtSidesDEBUG(world, basePos, baseBlock) : grassDirtSides(world, basePos, baseBlock);
    }

    /**
     * Checks whether given dirt block should be tinted by looking for a grass block without snow above it.
     *
     * @param world   RenderView used to get block positions.
     * @param basePos Position of the block to be checked.
     * @return True for tinting, false for no tinting.
     */
    public static boolean shouldBeTinted(@Nullable BlockRenderView world, @Nullable BlockPos basePos) {
        if (world == null || basePos == null) return false;
        BlockPos currentPos = basePos.up();
        BlockState currentBlock = world.getBlockState(currentPos);
        while (compareBlocks(currentBlock, Blocks.DIRT)) {
            currentPos = currentPos.up();
            currentBlock = world.getBlockState(currentPos);
        }
        return compareBlocks(currentBlock, Blocks.GRASS_BLOCK);
    }

    // This exists to not bork a working code. Any untested changes go here.
    public static dirtTexture grassDirtSidesDEBUG(@NotNull BlockRenderView world, @NotNull BlockPos basePos, @NotNull BlockState baseBlock) {
        boolean verbose = (basePos.getX() == VERBOSE_X && basePos.getY() == VERBOSE_Y && basePos.getZ() == VERBOSE_Z);
        if (verbose) log("found verbose block. name is " + baseBlock.getBlock().getName().toString());
        if (verbose) log("x " + basePos.getX() + " y " + basePos.getY() + " z " + basePos.getZ());
        //which sides are visible in original block
        boolean[] baseBlockSides = visibleSides(world, basePos, verbose);

        // if not visible, dont waste time
        if (verbose) log("Checking for visibility...");
        if (Arrays.equals(baseBlockSides, falseQuad)) {
            if (verbose) log("Failed.");
            return dirtTexture.DEFAULT;
        }
        if (verbose) log("Passed.");
        // check if blocks above are free from the same sides as OG block
        BlockPos currentPos = basePos.up();
        BlockState currentBlock = world.getBlockState(currentPos);
        while (compareBlocks(currentBlock, Blocks.DIRT)) {
            boolean[] sides = visibleSides(world, currentPos, verbose);
            if (!isVisibleOnOriginalSides(baseBlockSides, sides)) {
                if (verbose) log("Loop broken.");
                break;
            }
            if (verbose) log("Loop not broken. Moving up...");
            currentPos = currentPos.up();
            currentBlock = world.getBlockState(currentPos);
        }
        if (verbose) log("Loop ended.");
        //check whether top block is grass and has air above it, if loop above broken it will not pass either way
        if (world.isSkyVisible(currentPos.up())) {
            if (verbose) log("Top block can access sky");
            if (compareBlocks(currentBlock, Blocks.GRASS_BLOCK)) {
                if (verbose) log("Top block is regular grass");
                return dirtTexture.GRASSY;
            } else if (currentBlock == Blocks.GRASS_BLOCK.getDefaultState().with(net.minecraft.state.property.BooleanProperty.of("snowy"), true)) {
                if (verbose) log("Top block is snowy grass");
                return dirtTexture.SNOWY;
            } else {
                if (verbose) log("Top block is not grass");
                return dirtTexture.DEFAULT;
            }
        } else {
            if (verbose) log("Top block doesnt have access to sky");
            return dirtTexture.DEFAULT;
        }
    }

    // No touchy touchy, unless sure of changes
    @SuppressWarnings("unused")
    public static dirtTexture grassDirtSides(@NotNull BlockRenderView world, @NotNull BlockPos basePos, @NotNull BlockState baseBlock) {
        //which sides are visible in original block
        boolean[] baseBlockSides = visibleSides(world, basePos, false);

        // if not visible, dont waste time
        if (Arrays.equals(baseBlockSides, falseQuad)) {
            return dirtTexture.DEFAULT;
        }
        // check if blocks above are free from the same sides as OG block
        BlockPos currentPos = basePos.up();
        BlockState currentBlock = world.getBlockState(currentPos);
        while (compareBlocks(currentBlock, Blocks.DIRT)) {
            boolean[] sides = visibleSides(world, currentPos, false);
            if (!isVisibleOnOriginalSides(baseBlockSides, sides)) {
                break;
            }
            currentPos = currentPos.up();
            currentBlock = world.getBlockState(currentPos);
        }
        //check whether top block is grass and has air above it, if loop above broken it will not pass either way
        if (world.isSkyVisible(currentPos.up())) {
            if (compareBlocks(currentBlock, Blocks.GRASS_BLOCK)) {
                return dirtTexture.GRASSY;
            } else if (currentBlock == Blocks.GRASS_BLOCK.getDefaultState().with(net.minecraft.state.property.BooleanProperty.of("snowy"), true)) {
                return dirtTexture.SNOWY;
            } else {
                return dirtTexture.DEFAULT;
            }
        } else return dirtTexture.DEFAULT;
    }
}
