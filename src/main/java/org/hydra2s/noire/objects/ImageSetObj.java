package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.ImageCInfo;
import org.hydra2s.noire.descriptors.ImageSetCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageSubresourceRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.lwjgl.BufferUtils.createLongBuffer;
import static org.lwjgl.vulkan.VK10.*;

//
public class ImageSetObj extends BasicObj  {
    //
    public ArrayList<ImageObj> images = new ArrayList<ImageObj>();
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
        this.images = new ArrayList<ImageObj>();
        this.currentImageViews = new ArrayList<ImageViewObj>();
        this.previousImageViews = new ArrayList<ImageViewObj>();
        this.writingImageViews = new ArrayList<ImageViewObj>();

        //
        var Rs = cInfo.formats.length;
        for (var I=0;I<Rs;I++) {
            var fI = I;
            var imageCInfo = new ImageCInfo(){{
                memoryAllocator = cInfo.memoryAllocator;
                arrayLayers = cInfo.layerCounts.get(fI)*2;//*3;
                format = cInfo.formats[fI];
                mipLevels = 1;
                extent3D = cInfo.extents.get(fI);
                tiling = VK_IMAGE_TILING_OPTIMAL;
                samples = VK_SAMPLE_COUNT_1_BIT;
                usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                memoryAllocationInfo = new MemoryAllocationCInfo(){{
                    isHost = false;
                    isDevice = true;
                }};
            }};

            //
            var images = this.images;
            this.images.add(new ImageObj(this.base, imageCInfo));

            //
            this.currentImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "sampled";
                subresourceRange = VkImageSubresourceRange.calloc().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*0).layerCount(cInfo.layerCounts.get(fI));
            } }));

            //
            this.writingImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "storage";
                subresourceRange = VkImageSubresourceRange.calloc().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*1).layerCount(cInfo.layerCounts.get(fI));
            } }));

            //
            /*
            this.previousImageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){ {
                imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
                pipelineLayout = cInfo.pipelineLayout;
                image = images.get(fI).handle.get();
                type = "sampled";
                subresourceRange = VkImageSubresourceRange.calloc().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(cInfo.layerCounts.get(fI)*1).layerCount(cInfo.layerCounts.get(fI));
            } }));


            */
        }

        //
        this.handle = new Handle("ImageSet", MemoryUtil.memAddress(createLongBuffer(1)));
        deviceObj.handleMap.put$(this.handle, this);
    }

    //
    public ImageSetObj cmdBackstageId(VkCommandBuffer cmdBuf, int I) {
        CommandUtils.cmdCopyImageToImage(cmdBuf, currentImageViews.get(I).subresourceLayers(0), previousImageViews.get(I).subresourceLayers(0), ((ImageSetCInfo)this.cInfo).extents.get(I));
        return this;
    }

    //
    public ImageSetObj cmdSwapstageId(VkCommandBuffer cmdBuf, int I) {
        CommandUtils.cmdCopyImageToImage(cmdBuf, writingImageViews.get(I).subresourceLayers(0), currentImageViews.get(I).subresourceLayers(0), ((ImageSetCInfo)this.cInfo).extents.get(I));
        return this;
    }

    //
    public ImageSetObj cmdSwapstage(VkCommandBuffer cmdBuf) {
        var Is = images.size();
        for (var I=0;I<Is;I++) {
            this.cmdSwapstageId(cmdBuf, I);
        }
        return this;
    }

    //
    public ImageSetObj cmdBackstage(VkCommandBuffer cmdBuf) {
        var Is = images.size();
        for (var I=0;I<Is;I++) {
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
    public static class FramebufferObj extends ImageSetObj  {
        public ImageObj depthStencilImage = null;
        public ImageViewObj currentDepthStencilImageView = null;
        public ImageViewObj writingDepthStencilImageView = null;
        public ImageViewObj previousDepthStencilImageView = null;
        public ImageViewObj readingDepthStencilImageView = null;

        //
        public FramebufferObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public FramebufferObj(Handle base, ImageSetCInfo.FBLayout cInfo) {
            super(base, cInfo);

            //
            int layerCount = Collections.min(cInfo.layerCounts);

            //
            if (cInfo.depthStencilFormat != VK_FORMAT_UNDEFINED) {
                
                var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator).orElse(null);

                //
                this.depthStencilImage = new ImageObj(this.base, new ImageCInfo() {{
                    memoryAllocator = cInfo.memoryAllocator;
                    arrayLayers = layerCount * 2;
                    format = cInfo.depthStencilFormat;
                    mipLevels = 1;
                    extent3D = VkExtent3D.calloc().width(cInfo.scissor.extent().width()).height(cInfo.scissor.extent().height()).depth(1);
                    tiling = VK_IMAGE_TILING_OPTIMAL;
                    samples = VK_SAMPLE_COUNT_1_BIT;
                    usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = false;
                        isDevice = true;
                    }};
                }});

                //
                this.currentDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "sampled";
                        subresourceRange = VkImageSubresourceRange.calloc()
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(layerCount * 0)
                            .layerCount(layerCount);
                    }
                });

                //
                this.writingDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "storage";
                        subresourceRange = VkImageSubresourceRange.calloc()
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(layerCount * 1)
                            .layerCount(layerCount);
                    }
                });

                //
                this.readingDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "sampled";
                        subresourceRange = VkImageSubresourceRange.calloc()
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(layerCount * 1)
                            .layerCount(layerCount);
                    }
                });

                //
                /*this.previousDepthStencilImageView = new ImageViewObj(this.base, new ImageViewCInfo() {
                    {
                        imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL;
                        pipelineLayout = cInfo.pipelineLayout;
                        image = depthStencilImage.handle.get();
                        type = "sampled";
                        subresourceRange = VkImageSubresourceRange.calloc().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(layerCount * 1).layerCount(layerCount);
                    }
                });*/
            }
        }

        //
        @Override
        public ImageSetObj cmdBackstage(VkCommandBuffer cmdBuf) {
            super.cmdBackstage(cmdBuf);

            //
            var cInfo = ((ImageSetCInfo.FBLayout)this.cInfo);
            if (cInfo.depthStencilFormat != VK_FORMAT_UNDEFINED) {
                var extent = VkExtent3D.calloc().width(cInfo.scissor.extent().width()).height(cInfo.scissor.extent().height()).depth(1);
                CommandUtils.cmdCopyImageToImage(cmdBuf, currentDepthStencilImageView.subresourceLayers(0), previousDepthStencilImageView.subresourceLayers(0), extent);
            }

            //
            return this;
        }

        //
        @Override
        public ImageSetObj cmdSwapstage(VkCommandBuffer cmdBuf) {
            super.cmdSwapstage(cmdBuf);

            //
            var cInfo = ((ImageSetCInfo.FBLayout)this.cInfo);
            if (cInfo.depthStencilFormat != VK_FORMAT_UNDEFINED) {
                var extent = VkExtent3D.calloc().width(cInfo.scissor.extent().width()).height(cInfo.scissor.extent().height()).depth(1);
                CommandUtils.cmdCopyImageToImage(cmdBuf, writingDepthStencilImageView.subresourceLayers(0), currentDepthStencilImageView.subresourceLayers(0), extent);
            }

            return this;
        }

    }
}
