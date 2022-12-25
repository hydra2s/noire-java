package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.ImageSetCInfo;
import org.hydra2s.manhack.descriptors.ImageViewCInfo;
import org.hydra2s.manhack.descriptors.MemoryAllocationCInfo;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkOffset3D;

//
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

//
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.*;

//
public class ImageSetObj extends BasicObj  {
    //
    public ArrayList<MemoryAllocationObj.ImageObj> images = new ArrayList<MemoryAllocationObj.ImageObj>();
    public ArrayList<ImageViewObj> writingImageViews = new ArrayList<ImageViewObj>();
    public ArrayList<ImageViewObj> currentImageViews = new ArrayList<ImageViewObj>();
    public ArrayList<ImageViewObj> previousImageViews = new ArrayList<ImageViewObj>();

    //
    public ImageSetObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public ImageSetObj(Handle base, ImageSetCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
        var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);

        //
        this.images = new ArrayList<MemoryAllocationObj.ImageObj>();
        this.currentImageViews = new ArrayList<ImageViewObj>();
        this.previousImageViews = new ArrayList<ImageViewObj>();
        this.writingImageViews = new ArrayList<ImageViewObj>();

        //
        for (var I=0;I<cInfo.formats.capacity();I++) {
            //
            var allocationCInfo = new MemoryAllocationCInfo();
            allocationCInfo.isHost = false;
            allocationCInfo.isDevice = true;

            //
            var fI = I;
            var imageCInfo = new MemoryAllocationCInfo.ImageCInfo(){{
                arrayLayers = cInfo.layerCounts.get(fI)*3;
                format = cInfo.formats.get(fI);
                mipLevels = 1;
                extent3D = cInfo.extents.get(fI);
                tiling = VK_IMAGE_TILING_OPTIMAL;
                samples = VK_SAMPLE_COUNT_1_BIT;
                usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
            }};

            //
            var images = this.images;
            this.images.add((MemoryAllocationObj.ImageObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.ImageObj(this.base, imageCInfo)));

            //
            this.writingImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "storage";
                subresourceRange = VkImageSubresourceRange.create().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*0).baseArrayLayer(cInfo.layerCounts.get(fI));
            } }));

            //
            this.currentImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "sampled";
                subresourceRange = VkImageSubresourceRange.create().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*1).baseArrayLayer(cInfo.layerCounts.get(fI));
            } }));

            //
            this.previousImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "sampled";
                subresourceRange = VkImageSubresourceRange.create().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*2).baseArrayLayer(cInfo.layerCounts.get(fI));
            } }));
        }

        //
        this.handle = new Handle("ImageSet", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);
    }

    //
    public ImageSetObj cmdBackstageId(VkCommandBuffer cmdBuf, int I) {
        this.previousImageViews.get(I).cmdCopyImageViewToImageView(cmdBuf, (((ImageSetCInfo)this.cInfo).extents).get(I),
                VkOffset3D.create().x(0).y(0).z(0), 0, this.currentImageViews.get(I).handle.get(),
                VkOffset3D.create().x(0).y(0).z(0), 0);
        return this;
    }

    //
    public ImageSetObj cmdSwapstageId(VkCommandBuffer cmdBuf, int I) {
        this.currentImageViews.get(I).cmdCopyImageViewToImageView(cmdBuf, (((ImageSetCInfo)this.cInfo).extents).get(I),
                VkOffset3D.create().x(0).y(0).z(0), 0, this.writingImageViews.get(I).handle.get(),
                VkOffset3D.create().x(0).y(0).z(0), 0);
        return this;
    }

    //
    public ImageSetObj cmdSwapstage(VkCommandBuffer cmdBuf) {
        for (var I=0;I<images.size();I++) {
            this.cmdSwapstageId(cmdBuf, I);
        }
        return this;
    }

    //
    public ImageSetObj cmdBackstage(VkCommandBuffer cmdBuf) {
        for (var I=0;I<images.size();I++) {
            this.cmdBackstageId(cmdBuf, I);
        }
        return this;
    }

    //
    public <E> Stream<E> processCurrentImageViews(Function<ImageViewObj, E> handler) {
        return this.currentImageViews.stream().map(handler::apply);
    }

    //
    public <E> Stream<E> processPreviousImageViews(Function<ImageViewObj, E> handler) {
        return this.previousImageViews.stream().map(handler::apply);
    }

    //
    public <E> Stream<E> processWritingImageViews(Function<ImageViewObj, E> handler) {
        return this.writingImageViews.stream().map(handler::apply);
    }

    //
    public class FramebufferObj extends ImageSetObj  {
        public MemoryAllocationObj.ImageObj depthStencilImage = null;
        public ImageViewObj currentDepthStencilImageView = null;
        public ImageViewObj previousDepthStencilImageView = null;

        //
        public FramebufferObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public FramebufferObj(Handle base, ImageSetCInfo.FBLayout cInfo) {
            super(base, cInfo);

            //
            int layerCount = cInfo.layerCounts.stream().min(Integer::compare).get();

            //
            if (cInfo.depthStencilFormat != VK_FORMAT_UNDEFINED) {
                var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
                var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
                var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);

                //
                var allocationCInfo = new MemoryAllocationCInfo() {{
                    isHost = false;
                    isDevice = true;
                }};

                //
                var imageCInfo = new MemoryAllocationCInfo.ImageCInfo() {{
                    arrayLayers = layerCount * 2;
                    format = cInfo.depthStencilFormat;
                    mipLevels = 1;
                    extent3D = VkExtent3D.create().width(cInfo.scissor.extent().width()).height(cInfo.scissor.extent().height()).depth(1);
                    tiling = VK_IMAGE_TILING_OPTIMAL;
                    samples = VK_SAMPLE_COUNT_1_BIT;
                    usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
                }};

                //
                this.depthStencilImage = (MemoryAllocationObj.ImageObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.ImageObj(this.base, imageCInfo));

                //
                this.currentDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "sampled";
                        subresourceRange = VkImageSubresourceRange.create()
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(layerCount * 0)
                            .baseArrayLayer(layerCount);
                    }
                });

                //
                this.previousDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "sampled";
                        subresourceRange = VkImageSubresourceRange.create().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(layerCount * 1).baseArrayLayer(layerCount);
                    }
                });
            }
        }

        //
        @Override
        public ImageSetObj cmdBackstage(VkCommandBuffer cmdBuf) {
            super.cmdBackstage(cmdBuf);

            //
            var cInfo = ((ImageSetCInfo.FBLayout)this.cInfo);
            if (cInfo.depthStencilFormat != VK_FORMAT_UNDEFINED) {
                var extent = VkExtent3D.create().width(cInfo.scissor.extent().width()).height(cInfo.scissor.extent().height()).depth(1);

                //
                this.currentDepthStencilImageView.cmdCopyImageViewToImageView(cmdBuf, extent,
                        VkOffset3D.create().x(0).y(0).z(0), 0, this.previousDepthStencilImageView.handle.get(),
                        VkOffset3D.create().x(0).y(0).z(0), 0);
            }

            //
            return this;
        }

        //
        @Override
        public ImageSetObj cmdSwapstage(VkCommandBuffer cmdBuf) {
            super.cmdSwapstage(cmdBuf);
            return this;
        }

    }
}
