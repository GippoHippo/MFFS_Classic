package com.mffs.common.tile.type;

import com.builtbroken.mc.imp.transform.vector.Location;
import com.mffs.ModularForcefieldSystem;
import com.mffs.SettingConfiguration;
import com.mffs.api.card.ICardInfinite;
import com.mffs.api.card.ICoordLink;
import com.mffs.api.fortron.FrequencyGrid;
import com.mffs.api.fortron.IFortronCapacitor;
import com.mffs.api.fortron.IFortronFrequency;
import com.mffs.api.modules.IModule;
import com.mffs.api.utils.FortronHelper;
import com.mffs.api.vector.Vector3D;
import com.mffs.common.TransferMode;
import com.mffs.common.items.card.ItemCardFrequency;
import com.mffs.common.items.card.ItemCardLink;
import com.mffs.common.items.modules.upgrades.ItemModuleScale;
import com.mffs.common.items.modules.upgrades.ItemModuleSpeed;
import com.mffs.common.net.packet.EntityToggle;
import com.mffs.common.tile.TileModuleAcceptor;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Calclavia
 */
public class TileFortronCapacitor extends TileModuleAcceptor implements IFortronCapacitor
{

    /* Current distribution method */
    private TransferMode mode = TransferMode.EQUALIZE;

    /**
     * Constructor.
     */
    public TileFortronCapacitor()
    {
        this.capacityBase = 700;
        this.capacityBoost = 10;
        this.module_index = 2;
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();

        if (this.isActive())
        {
            int cost = getFortronCost() + SettingConfiguration.BASE_POWER_CONSUMPTION_CAPACITOR;
            if (cost > 0)
            {
                requestFortron(cost, true);
            }

            //TODO: Change the draining to remove X% of transfered fortron.
            if (this.ticks % 10 == 0)
            { //cannot run if there is 0 energy!
                Set<IFortronFrequency> connected = new HashSet<>();
                for (ItemStack stack : getCards())
                {
                    if (stack == null)
                    {
                        continue;
                    }

                    if (stack.getItem() instanceof ICardInfinite)
                    {
                        setFortronEnergy(getFortronCapacity());
                    }
                    else if (stack.getItem() instanceof ICoordLink)
                    {
                        Location link = ((ICoordLink) stack.getItem()).getLink(stack);
                        if (link != null)
                        {
                            TileEntity link_machine = link.getTileEntity(this.worldObj);
                            if (link_machine instanceof IFortronFrequency)
                            {
                                connected.add(this);
                                connected.add((IFortronFrequency) link_machine);
                            }
                        }
                    }
                }
                if (connected.isEmpty())
                {
                    getLinkedDevices(connected);
                }

                FortronHelper.transfer(this, connected, mode, getTransmissionRate());
            }
        }
    }

    /**
     * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
     *
     * @param slot
     * @param stack
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack)
    {
        if (slot == 0)
        {
            return stack.getItem() instanceof ICardInfinite || stack.getItem() instanceof ItemCardLink;
        }
        else if (slot == 1)
        {
            return stack.getItem() instanceof ItemCardFrequency || stack.getItem() instanceof ItemCardLink;
        }
        return stack.getItem() instanceof IModule;
    }

    /**
     * @return
     */
    @Override
    public Set<ItemStack> getCards()
    {
        Set<ItemStack> set = new HashSet<>();
        set.add(super.getCard());
        set.add(getStackInSlot(1));
        return set;
    }

    @Override
    public void getLinkedDevices(Set<IFortronFrequency> list)
    {
        list.addAll(FrequencyGrid.instance().getFortronTilesExcluding(this, new Vector3D(this), getTransmissionRange(), getFrequency()));
    }

    @Override
    public int getTransmissionRange()
    {
        return 15 + getModuleCount(ItemModuleScale.class);
    }

    @Override
    public int getTransmissionRate()
    {
        return 250 + 50 * getModuleCount(ItemModuleSpeed.class);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setByte("transferMode", (byte) mode.ordinal());
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.mode = TransferMode.values()[nbt.getByte("transferMode")];
    }

    @Override
    public int getSizeInventory()
    {
        return 5;
    }

    public TransferMode getTransferMode()
    {
        return this.mode;
    }

    @Override
    public float getAmplifier()
    {
        return .001F;
    }

    /**
     * Handles the message given by the handler.
     *
     * @param imessage The message.
     */
    @Override
    public IMessage handleMessage(IMessage imessage)
    {
        if (imessage instanceof EntityToggle)
        {
            EntityToggle tog = (EntityToggle) imessage;
            if (tog.toggle_opcode == EntityToggle.TRANSFER_TOGGLE)
            {
                this.mode = this.mode.toggle();
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                return null;
            }
        }
        return super.handleMessage(imessage);
    }

    @Override
    public List<ItemStack> getRemovedItems(EntityPlayer entityPlayer)
    {
        List<ItemStack> stack = super.getRemovedItems(entityPlayer);
        stack.add(new ItemStack(ModularForcefieldSystem.fortronCapacitor));
        return stack;
    }
}
