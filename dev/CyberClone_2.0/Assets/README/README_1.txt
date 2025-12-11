================================================================================
CYBERCLONE - ASSET ATTRIBUTION README
================================================================================
Student: Riyaz Ismail (2215624)
CS3005 Coursework A | November 2025
================================================================================

QUICK REFERENCE GUIDE FOR EXTERNAL RESOURCES

================================================================================
KEY YOUTUBE TUTORIALS
================================================================================

Tilemap & Collision:
└─ Velvary - "Adding Collisions For Your Tilemaps and 2D Character"
   https://www.youtube.com/watch?v=c2oSktomBHLW4
└─ Dani Krossing - "HOW TO CREATE TILEMAPS IN UNITY"
   https://youtu.be/tEQw-YKQndB?si=ML_XfYCkekM_JungZWe

Player Movement:
└─ Sasquatch B Studios - "TOP DOWN Movement - Unity Tutorial"
   https://www.youtube.com/watch?v=RN3yuCvzsL4
└─ Unity - "Creating 2D Player Movement for Isometric Games"
   (Official Tutorial, 136K views)

Animation:
└─ Penutbumlo - "INSANELY Smooth pixel ATTACK Animation Tutorial"
   https://www.youtube.com/watch?v=KgSSTa-TFRc
└─ CasanisPlays - "2D Prototyping in Unity - Player Animation"

Isometric Setup:
└─ Syloe - "MAKING ISOMETRIC TILEMAP in Unity 2019!"
   https://youtu.be/IW7f4Zg21YY
└─ Unity - "How to use Isometric Tilemap in Unity 2018.3!"
   (Official Tutorial, 334K views)

C# Fundamentals:
└─ Unity - "C# Enumerations in Unity!"
   https://youtu.be/LAE2aBCMYlw
└─ Code Monkey - "Use Enums for State handling (DON'T USE STRINGS)"
   (53K views)
└─ Contraband - "4 Unity C# Coding Tips!"
   (SerializeField and Inspector tips)
└─ Game Dev Beginner - "Coroutines in Unity"
   https://youtu.be/kIJ2V0rQGnM
└─ Tofami - "Preprocessor Directives in Unity"
   https://youtu.be/auTOHvOfGwM

AI State Machines:
└─ Devduff - "State Based AI Behavior using C#"
   https://youtu.be/lqt_LkkMyNPc4

Audio Implementation:
└─ Sasquatch B Studios - "How To Add Sound Effects the RIGHT Way"
   (99K views)

Unity 6 Updates:
└─ Sunny Valley Studio - "Unity 6 REMOVED velocity from Rigidbody2D!"
   (linearVelocity migration)

Build & Export:
└─ United Top Tech - "How to Export Unity Game to Windows Desktop"
└─ Unity - "How to Export your Unity Game and Play it on Your Desktop"
   (Official Tutorial, 183K views)

================================================================================
AUDIO
================================================================================

Epidemic Sound - https://www.epidemicsound.com
└─ Background music, footstep sounds, attack SFX, enemy audio

================================================================================
VISUAL TOOLS
================================================================================

Aseprite - https://www.aseprite.org
└─ All pixel art sprites and animations

Unity 2D Aseprite Importer (v1.0.1)
└─ Package: com.unity.2d.aseprite
└─ Automated .ase/.aseprite import with layer parsing and spritesheet generation
└─ Docs: https://docs.unity3d.com/Packages/com.unity.2d.aseprite@1.0/

================================================================================
DEVELOPMENT ENVIRONMENT
================================================================================

Unity Engine 2022.3+ LTS
Visual Studio 2022 / IntelliJ IDEA
Unity Documentation - https://docs.unity3d.com

================================================================================
IMPLEMENTATION NOTES
================================================================================

Scene Management (attemptToDesignGameFlow.cs):
└─ Singleton pattern + coroutine-based transitions
└─ Based on coroutine/preprocessor tutorials, implemented by me

Boss AI (isoBossController.cs):
└─ Finite state machine with enum states
└─ Adapted from state machine tutorial methodology

Player Controller:
└─ WASD + mouse input adapted for isometric projection
└─ Based on top-down movement tutorial

Collision System:
└─ Tilemap Collider 2D + Composite Collider 2D
└─ Configuration from tilemap tutorial

Animation System:
└─ Manual clip creation in Unity using Aseprite frames
└─ Input magnitude-based transitions (debugged by me)

================================================================================
ORIGINAL STUDENT WORK
================================================================================

✓ All C# script implementations (adapted from tutorial concepts)
✓ Complete level design and layout
✓ Enemy/boss behaviour patterns and balance
✓ All pixel art assets in Aseprite
✓ Game design decisions and progression
✓ UI implementation
✓ Debugging and optimisation

================================================================================
ACADEMIC INTEGRITY
================================================================================

All external resources properly attributed. Tutorial methodologies used as
learning references and adapted for project requirements. AI used for
educational support only - all code written by me after understanding
concepts. Project complies with Brunel University academic integrity policies.

================================================================================
AI TOOLS
================================================================================

Claude AI (Anthropic) - https://claude.ai
└─ Technical guidance, architectural explanations, debugging support
└─ No complete code generated - used as educational reference only
└─ Helped formulate search terms and explain Unity API methods

================================================================================
For complete attribution details, see CREDITS.txt
================================================================================
