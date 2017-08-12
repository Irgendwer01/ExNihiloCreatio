package exnihilocreatio.tiles;

import exnihilocreatio.config.Config;
import exnihilocreatio.rotationalPower.IRotationalPowerConsumer;
import exnihilocreatio.rotationalPower.IRotationalPowerMember;
import exnihilocreatio.util.BlockInfo;
import exnihilocreatio.util.Util;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import java.util.HashSet;

public class TileAutoSifter extends BaseTileEntity implements ITickable, IRotationalPowerConsumer{
    public TileSieve[][] toSift = null;
    public boolean[][] connectionPieceMap = null;

    public EnumFacing facing = EnumFacing.NORTH;
    public int tickCounter = 0;
    public float rotationValue = 0;
    public float perTickRotation = 0;

    public float storedRotationalPower = 0;

    public float offsetX = 0;
    public float offsetY = 0;
    public float offsetZ = 0;

    public ItemHandlerAutoSifter itemHandlerAutoSifter;

    public TileAutoSifter() {
        itemHandlerAutoSifter = new ItemHandlerAutoSifter();
        itemHandlerAutoSifter.setTe(this);
    }

    @Override
    public void update() {
        tickCounter++;

        if (world.isRemote && perTickRotation != 0){
            float r = 0.1F;
            float cx = 0F;

            offsetX = cx + r * (float)Math.cos(tickCounter);
        }

        if (tickCounter % 10 == 0){
            perTickRotation = calcEffectivePerTickRotation(facing);

            BlockPos posOther = pos.up();
            TileEntity te = world.getTileEntity(posOther);

            if (te != null && te instanceof TileSieve) {
                toSift = collectPossibleSieves((TileSieve) te);
            } else {
                toSift = null;
            }
        }

        storedRotationalPower += perTickRotation;

        if (Math.abs(storedRotationalPower) > 100 &&  toSift != null){
            storedRotationalPower += storedRotationalPower > 0 ? -100 : 100;
            doAutoSieving(toSift);
        }


        if (world.isRemote){
            rotationValue += perTickRotation;

        }
    }

    private void calculateConnectionPieces(){ //TODO
        if (toSift != null){
            connectionPieceMap = new boolean[toSift.length][toSift.length];

            for (int x = 0; x < toSift.length - 1; x++) {
                for (int z = 0; z < toSift.length - 1; z++) {
                    if (toSift[x][z] == null){
                        connectionPieceMap[x][z] = false;
                        connectionPieceMap[x][z] = false;
                    }
                }
            }
        }
    }

    private void checkValidSieves(){
        // toSift.clear();

        HashSet<TileSieve> visited = new HashSet<>();


        // Has to be placed below a sieve
        BlockPos posOther = pos.up();
        TileEntity te = world.getTileEntity(posOther);

        if (te != null && te instanceof TileSieve) {
            TileSieve sieve = (TileSieve) te;

            TileSieve[][] sieveMap = collectPossibleSieves(sieve);

            doAutoSieving(sieveMap);
        }
    }

    private void cleanUnconnected(TileSieve thisSieve, TileSieve[][] sieveMap){
        for (int xCoord = 0; xCoord < sieveMap.length; xCoord++) {
            for (int zCoord = 0; zCoord < sieveMap.length; zCoord++) {
                if (!(sieveMap[xCoord][zCoord] == null)){

                }
            }
        }


    }

    private boolean checkNeighboursConnectedToMain(TileSieve mainSieve, TileSieve[][] sieveMap, int xCoord, int zCoord){
        int bounds = Config.autoSieveRadius  * 2 + 1;

        int notConnected = 0;

        // check left
        if (xCoord -1 >= 0 && xCoord -1 <= bounds){
            if (sieveMap[xCoord -1][zCoord] == null){
                notConnected++;
            }else {
                if (sieveMap[xCoord -1][zCoord] == mainSieve){
                    return true;
                }else {
                    return checkNeighboursConnectedToMain(mainSieve, sieveMap, xCoord -1, zCoord);
                }
            }
        }else {
            notConnected++;
        }

        if (notConnected == 4){
            sieveMap[xCoord][zCoord] = null;
        }

        return false;
    }

    private TileSieve[][] collectPossibleSieves(TileSieve thisSieve){
        BlockPos sievePos = thisSieve.getPos();

        TileSieve[][] sieveMap = new TileSieve[Config.autoSieveRadius  * 2 + 1][Config.autoSieveRadius  * 2 + 1];

        for (int xOffset = -1 * Config.autoSieveRadius; xOffset <= Config.autoSieveRadius; xOffset++) {
            for (int zOffset = -1 * Config.autoSieveRadius; zOffset <= Config.autoSieveRadius; zOffset++) {
                sieveMap[xOffset + Config.autoSieveRadius][zOffset + Config.autoSieveRadius] = null;

                TileEntity entity = world.getTileEntity(sievePos.add(xOffset, 0, zOffset));
                if (isValidPartnerSieve(thisSieve, entity)){
                    sieveMap[xOffset + Config.autoSieveRadius][zOffset + Config.autoSieveRadius] = (TileSieve) entity;
                }

            }
        }

        return sieveMap;
    }

    private boolean isValidPartnerSieve(TileSieve thisSieve, TileEntity tileOther){
        if (tileOther != null && tileOther instanceof TileSieve){

            TileSieve sieve = (TileSieve) tileOther;
            sieve.validateAutoSieve();

            if (sieve.autoSifter == this) {
                return true;
            } else if (sieve.autoSifter == null) {
                sieve.autoSifter = this;
                return true;
            }
        }
        return false;
    }

    private void doAutoSieving(TileSieve[][] sieveMap) {
        for (TileSieve[] tileSieves : sieveMap) {
            for (TileSieve tileSieve : tileSieves) {
                if (tileSieve != null) {
                    if (!itemHandlerAutoSifter.getStackInSlot(0).isEmpty() && tileSieve.addBlock(itemHandlerAutoSifter.getStackInSlot(0))){
                        itemHandlerAutoSifter.getStackInSlot(0).shrink(1);
                    }
                    tileSieve.doSieving(null, true);
                }
            }
        }
    }

    @Override
    public float getMachineRotationPerTick() {
        return perTickRotation;
    }

    @Override
    public void setEffectivePerTickRotation(float rotation) {
        perTickRotation = rotation;
    }

    private float calcEffectivePerTickRotation(EnumFacing direction) {
        if (facing == direction) {
            BlockPos posProvider = pos.offset(facing.getOpposite());
            TileEntity te = world.getTileEntity(posProvider);
            if (te != null && te instanceof IRotationalPowerMember) {
                return ((IRotationalPowerMember) te).getEffectivePerTickRotation(facing) + getOwnRotation();
            } else return getOwnRotation();
        } else return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) itemHandlerAutoSifter;
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ||
                super.hasCapability(capability, facing);
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        // if (currentItem != null) {
        //     NBTTagCompound currentItemTag = currentItem.writeToNBT(new NBTTagCompound());
        //     tag.setTag("currentItem", currentItemTag);
        // } TODO: implement


        NBTTagCompound itemHandlerTag = itemHandlerAutoSifter.serializeNBT();
        tag.setTag("itemHandler", itemHandlerTag);

        if (facing != null)
            tag.setString("facing", facing.getName());

        tag.setFloat("rot", rotationValue);
        tag.setFloat("sRot", storedRotationalPower);

        return super.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        // if (tag.hasKey("currentItem")) {
        //     currentItem = ItemInfo.readFromNBT(tag.getCompoundTag("currentItem"));
        // } else {
        //     currentItem = null;
        // } TODO: see above

        if (tag.hasKey("itemHandler")) {
            itemHandlerAutoSifter.deserializeNBT((NBTTagCompound) tag.getTag("itemHandler"));
        }

        if (tag.hasKey("facing"))
            facing = EnumFacing.byName(tag.getString("facing"));

        if (tag.hasKey("rot"))
            rotationValue = tag.getFloat("rot");

        if (tag.hasKey("sRot"))
            storedRotationalPower = tag.getFloat("sRot");


        super.readFromNBT(tag);
    }

    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getTexture() {
        if (!itemHandlerAutoSifter.getStackInSlot(0).isEmpty()) {
            return Util.getTextureFromBlockState(new BlockInfo(itemHandlerAutoSifter.getStackInSlot(0)).getBlockState());
            //TODO: MOVE BLOCKINFO OUT -> should not get recreated each frame
        }

        return null;
    }
}
