package com.mffs.common.items.modules.interdiction;

import com.mffs.api.security.IInterdictionMatrix;
import com.mffs.common.items.modules.ItemMatrixModule;
import com.mffs.common.items.modules.projector.ModuleShock;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.INpc;
import net.minecraft.entity.monster.IMob;

/**
 * @author Calclavia
 */
public class ModuleAntiFriendly extends ItemMatrixModule {

    @Override
    public boolean onDefend(IInterdictionMatrix paramIInterdictionMatrix, EntityLivingBase paramEntityLivingBase) {
        if (paramEntityLivingBase instanceof INpc || !(paramEntityLivingBase instanceof IMob)) {
            paramEntityLivingBase.setHealth(1F);
            paramEntityLivingBase.attackEntityFrom(ModuleShock.SHOCK_SOURCE, 100F);
        }
        return false;
    }
}