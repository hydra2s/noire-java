# 👩‍🎤 Noire.java 👩‍🎤

New renderer engine for Java and Minecraft mod. Also, this is, probably, final project of and in my life (i.e. finale of my 2023 year).

### 🐞 Status of Debug 🐞

I was able to run both GFXreconstruct (but with a flag) and NVIDIA Nsight. There is a problem with the last one, because I can't export it. And with the first one, the problem is with the exorbitant file size (almost a gigabyte). Also, GFXreconstruct (replay) just plays at a very fast speed, as if under rewind. In general, I managed to achieve compatibility with stable drivers from NVIDIA a few days ago.  So I had a tremendous amount of data to analyze, but I lost it.

## About drama around [VulkanModHybrid](https://github.com/hydra2s/VulkanModHybrid) and [VulkanMod](https://github.com/xCollateral/VulkanMod) project

I did not want to mislead anyone, I even changed the name a bit in VMH, yes I took as a basis, and besides I wanted to fix a lot of problems with the original mod. I did not want to engage in self-promotion... Plus I wanted to make something that the original mod probably was not, and probably will not be for a long time. I wanted to apologize for everything. Yes, I admit that this is not the official version, a fan build.

And now I will be forced to withdraw from production test snapshots and builds, testing is over and went badly, needs revision. For all questions about this mod, please do not contact the authors of the original VulkanMod. And vice versa, for questions about the original, it is better not to contact me yet.

Also, I may stop supporting this fork or unofficial version at any time. Depending on whether I have enough resources to develop and support it. At the moment, the pace of development has slowed down a lot, and even I myself can see that. And besides, I have circumstances, and I'm also very tired of all this already.

As for the version, yes, I probably made the dumbest mistake in that regard. There were just so many changes that I wanted to give that meaning. My misguided actions have caused sway and heat, and a potential threat to the hype of both projects. The original version of VulkanMod alone got 10,000 downloads and extra stars on Github (essentially unwitting advertising for both projects). As well as additional waves of forks (including in the future). And this is actually bad, because in addition to positive reactions, it also entails negative reactions and evaluations, as well as unnecessary and/or undesirable criticism. I really didn't want to do any black PR or anti-advertising on this whole thing. 

Earlier I wanted to make a separate confession about the use of code. Actually, not all source code is exclusively mine (including external libraries or some elements outside "noire.java", they are even labeled differently than the mine). Yes, I may have forgotten to add labels about where the author's code is (in the case of VulkanMod) and where my corrections or innovations are. I'll try to fix that in the future. 

Based on all of the above, I give permission to use my code and my work. I give you my rights and permissions to do so, as well as the distribution in the final or intermediate products. But you should understand that there is my version and there is a version from third-party developers, and they may have their own rights. 

## Features (incomplete list)

- 📱 [Vulkan API 1.3.236](https://registry.khronos.org/vulkan/)
- 📱 Unified memory, descriptor sets, layout, etc.

### Gen-v2.0 almost done

- 🌱 Timeline semaphores (finally).
- 🌱 Full-scale update - more FPS in some cases.
- 🌱 Queue groups instead of queue family.
- 🌱 Per-queue awaiting semaphores instead of queue family.
- 🌱 Semaphore v2 (timeline), queue submit v2, and other Vulkan 1.3 features...
- 🌱 Command buffer manager, managment, writer, new utils, etc.
- 🌱 Multiple command pools (per queue group).
- 🌱 Mass refactoring, refinements.
- 🌱 Frame skipping support (much more FPS or TPS).
- 🌱 Multi-draw centric and oriented.

### TODO Gen-2.0.1

- 📌 Conditional rendering (from buffer, WIP), occlusion query (to buffer, WIP). 
- 📌 New and better command agent system (WIP).
- 📌 Full stencil support (WIP).
- 📌 Additional legacy support (vendor-related).
- 📌 Optional ray-tracing (and enabled).
- 📌 New draw-grouping and collection.
- 📌 Fix common issues and problems.
- 📌 Probably, rework swapchain system.
- 📌 Feature set for SSLR, shadows, etc.
- 📌 Feature set for ray-tracing (chunks, entity, etc.). 
- 📌 Dynamically configurable. 
- 📌 More feature checking (for MultiDraw, etc.).

## Timing of development

💔 Rather extremely long, could take as long as 2024. Although we and I need to hurry, and do it as fast as we can. The contract is actually up to two years. So I would really like to find maintainers, partners, workers, developers, up to 5 people. At this point, I have to and do act alone. Given our situation, we have to act solely on pure enthusiasm, as well as for the sake of the portfolio. It's just that I need something to answer to God in the afterlife. 💔
