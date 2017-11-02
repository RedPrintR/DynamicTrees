package com.ferreusveritas.dynamictrees.worldgen;

import java.util.HashMap;
import java.util.Random;

import com.ferreusveritas.dynamictrees.api.worldgen.IBiomeDensityProvider;
import com.ferreusveritas.dynamictrees.trees.DynamicTree;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

public class DefaultBiomeDensityProvider implements IBiomeDensityProvider {

	private interface IChance {
		EnumChance getChance(Random random, int radius);
	}

	private class ChanceStatic implements IChance {
		private final EnumChance chance;
		
		public ChanceStatic(EnumChance chance) {
			this.chance = chance;
		}
		
		@Override
		public EnumChance getChance(Random random, int radius) {
			return chance;
		}
	}

	private class ChanceRandom implements IChance {
		private final float value;
		
		public ChanceRandom(float value) {
			this.value = value;
		}
		
		@Override
		public EnumChance getChance(Random random, int radius) {
			return random.nextFloat() < value ? EnumChance.OK : EnumChance.CANCEL;
		}
	}
	
	private class ChanceByRadius implements IChance {
		@Override
		public EnumChance getChance(Random random, int radius) {
			float chance = 1.0f;
			
			if(radius > 3) {//Start dropping tree spawn opportunities when the radius gets bigger than 3
				chance = 2.0f / radius;
				return random.nextFloat() < chance ? EnumChance.OK : EnumChance.CANCEL;
			}

			return random.nextFloat() < chance ? EnumChance.OK : EnumChance.CANCEL;
		}
	}
		
	HashMap<Integer, IChance> fastChanceLookup = new HashMap<Integer, IChance>();
	
	@Override
	public String getName() {
		return "default";
	}
	
	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public double getDensity(Biome biome, double noiseDensity, Random random) {
		
		if(BiomeDictionary.isBiomeOfType(biome, Type.SPOOKY)) { //Roofed Forest
			if(random.nextInt(4) == 0) {
				return 1.0f;
			}
			if(random.nextInt(8) == 0) {
				return 0.0f;
			}
			return (noiseDensity * 0.25) + 0.25;
		}
		
		double naturalDensity = MathHelper.clamp_float((biome.theBiomeDecorator.treesPerChunk) / 10.0f, 0.0f, 1.0f);//Gives 0.0 to 1.0
		return noiseDensity * (naturalDensity * 1.5f);
	}
	
	@Override
	public EnumChance chance(Biome biome, DynamicTree tree, int radius, Random random) {

		int biomeId = Biome.getIdForBiome(biome);
		IChance chance;
		
		if(fastChanceLookup.containsKey(biomeId)) {
			chance = fastChanceLookup.get(biomeId);
		} else {
			if(BiomeDictionary.isBiomeOfType(biome, Type.FOREST)) {//Never miss a chance to spawn a tree in a forest.
				chance = new ChanceStatic(EnumChance.OK);
			}
			else if(BiomeDictionary.isBiomeOfType(biome, Type.SWAMP)) {//Swamps need more tree opportunities since it's so watery
				chance = new ChanceRandom(0.5f);
			} 
			else if(biome.theBiomeDecorator.treesPerChunk == -999) {//Deserts, Mesas, Beaches
				chance = new ChanceStatic(EnumChance.CANCEL);
			}
			else {
				chance = new ChanceByRadius();//Let the radius determine the chance
			}

			fastChanceLookup.put(biomeId, chance);
		}
		
		//the last call should never be UNHANDLED for the DefaultBiomeDensityProvider since it is the last in the chain
		return chance.getChance(random, radius);
	}
	
}
