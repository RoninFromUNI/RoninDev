
using UnityEngine;


//had to learn enumerations to establish the up down left right and inbetweens
//for 8 direction sprites
public enum Direction
{
    South = 0,      // Down
    SouthEast = 1,  // Down-Right
    East = 2,       // Right
    NorthEast = 3,  // Up-Right
    North = 4,      // Up
    NorthWest = 5,  // Up-Left
    West = 6,       // Left
    SouthWest = 7   // Down-Left
}

/// <summary>
/// Isometric 2D character controller with 8-directional sprite support
/// Handles WASD movement, sprite flipping, and animation state management
/// </summary>

public class IsometricCharacterController1 : MonoBehaviour
{
    [Header("Movement Settings")]
    [SerializeField]
    [Tooltip("Movement speed in units per second")]
    private float movementSpeed = 5f;

    [SerializeField]
    [Tooltip("Deceleration factor when no input (0=instant stop, 1=no friction)")]
    private float movementSmoothing = 0.85f;
    /// <summary>
    /// 
    /// 
    /// anxiety removing space here
    /// 
    /// 
    /// </summary>
    [Header("Isometric Configuration")]
    [SerializeField]
    [Tooltip("Vertical compression ratio for isometric projection (0.5 = standard)")]
    private float isometricYScale = 0.5f;
    
    [SerializeField]
    [Tooltip("Normalize diagonal movement to prevent faster diagonal speed")]
    private bool normalizeDiagonalSpeed = true;
    ///
    ///
    ///
    [Header("Camera Boundaries")]
    [SerializeField]
    [Tooltip("Enable position clamping to boundaries")]
    private bool useBoundaries = false;
    
    [SerializeField]
    private float minx = -10f;
    
    [SerializeField]
    private float maxx = 10f;
    
    [SerializeField]
    private float miny = -10f;
    
    [SerializeField]
    private float maxy = 10f;
    ///
    ///
    ///
    ///
    ///
    [Header("Sprite Direction System")]
    [SerializeField]
    [Tooltip("Use 8-directional sprites (includes diagonals)")]
    private bool use8DirectionalSprites = true;
    
    [SerializeField]
    [Tooltip("Flip sprite horizontally for left-facing directions (saves animation work)")]
    private bool useHorizontalFlipping = true;
    ///
    ///
    ///
    ///
    ///
    [Header("Animation Integration")]
    [SerializeField]
    [Tooltip("Animator component - auto-finds if not assigned")]
    private Animator spriteAnimator;
    
    [SerializeField]
    [Tooltip("Name of Speed float parameter in Animator")]
    private string speedParameterName = "Speed";
    
    [SerializeField]
    [Tooltip("Name of Direction integer parameter in Animator (0-7 for 8 directions)")]
    private string directionParameterName = "Direction";

    [SerializeField]
    [Tooltip("Name of IsMoving bool parameter in Animator (Idle <-> Running)")]
    private string isMovingParameterName = "IsMoving";
    /// <summary>
    /// 
    /// 
    /// 
    /// sound finally 
    /// </summary>
    [Header("DWAYNE | AUDIO")]
    [SerializeField]
    [Tooltip("audiosrc for movement sounds like footsteps and landing")]
    private AudioSource movementAudioSrc;

    [SerializeField]
    [Tooltip("basic array of sounds variations")]
    private AudioClip[] footstepsClips;

    [SerializeField]
    [Tooltip("setting a time interval editor")]
    private float footstepTimeInterval = 0.5f;

    [SerializeField]
    [Tooltip("pitch variation could work too")]
    private float pitchVar = 0.1f;

    [SerializeField]
    [Tooltip("threshold speed to trigger the foot step, i think its best to match it to the animation discipline")]
    private float footstepthreshold = 0.3f;


    /// <summary>
    /// 
    /// 
    /// So just really need to adjust my private variables and then
    /// continue with what i got with scene loading
    /// 
    /// 
    /// </summary>
    private Rigidbody2D rb2d;
    private float footstepTimer = 0f;
    private SpriteRenderer spriteRenderer;
    private Vector2 movementInput;
    private Vector2 currentVelocity;
    private int currentDirection = 0;
    private bool isFacingRight = true;
    
    void Start()
    {
        InitializeComponents();
    }
    
    void Update()
    {
        GatherInput();
        FootStepAudio(); //calling this function to update for now
    }
    
    void FixedUpdate()
    {
        ApplyMovement();
        ClampPositionToBoundaries();
    }
    
    void LateUpdate()
    {
        UpdateSpriteDirection();
        UpdateAnimationState();
    }
    
    /// <summary>
    /// Initialize required component references
    /// </summary>
    private void InitializeComponents()
    {
        rb2d = GetComponent<Rigidbody2D>();
        if (rb2d == null)
        {
            rb2d = gameObject.AddComponent<Rigidbody2D>();
        }
        
        // Configure Rigidbody2D for character controller
        rb2d.gravityScale = 0f;
        rb2d.constraints = RigidbodyConstraints2D.FreezeRotation;
        rb2d.collisionDetectionMode = CollisionDetectionMode2D.Continuous;
        rb2d.interpolation = RigidbodyInterpolation2D.Interpolate;
        
        spriteRenderer = GetComponent<SpriteRenderer>();
        
        if (spriteAnimator == null)
        {
            spriteAnimator = GetComponent<Animator>();
        }
    }

    /// <summary>
    /// Gather WASD input and convert to isometric movement vector
    /// </summary>
    private void GatherInput()
    {
        float horizontal = Input.GetAxisRaw("Horizontal");
        float vertical = Input.GetAxisRaw("Vertical");

        // Apply isometric Y compression
        movementInput = new Vector2(horizontal, vertical * isometricYScale);

        // Normalize diagonal movement if enabled
        if (normalizeDiagonalSpeed && movementInput.magnitude > 1f)
        {
            movementInput.Normalize();
        }
    }
    
    private void FootStepAudio()
{
    if (movementAudioSrc == null || footstepsClips.Length == 0) return;
    
    float normalizedSpeed = GetNormalizedSpeed();
    
    if (normalizedSpeed > footstepthreshold)
    {
        footstepTimer -= Time.deltaTime;
        
        if (footstepTimer <= 0)
        {
            footstepTimer = footstepTimeInterval / normalizedSpeed;
            
            
            AudioClip selectedClip = footstepsClips[Random.Range(0, footstepsClips.Length)];
            
            movementAudioSrc.pitch = 1f + Random.Range(-pitchVar, pitchVar);
            
            //note to self: dont make local variable match usage or array access is buns
            movementAudioSrc.PlayOneShot(selectedClip);
        }
    }
    else
    {
        footstepTimer = 0f;
    }
}

    /// <summary>
    /// Apply movement physics with smoothing
    /// </summary>
    private void ApplyMovement()
    {
        if (movementInput.magnitude > 0f)
        {
            Vector2 targetVelocity = movementInput * movementSpeed;
            currentVelocity = Vector2.Lerp(currentVelocity, targetVelocity, 10f * Time.fixedDeltaTime);
        }
        else
        {
            // Apply deceleration
            currentVelocity *= movementSmoothing;

            if (currentVelocity.magnitude < 0.01f)
            {
                currentVelocity = Vector2.zero;
            }
        }

        rb2d.linearVelocity = currentVelocity;
    }
    
    /// <summary>
    /// 
    /// 
    /// for this i would have like a min and max x and y boundary to play with in a 
    /// represented gizmos box, very fun
    /// </summary>
    private void ClampPositionToBoundaries()
    {
        if (!useBoundaries) return;
        
        Vector3 position = transform.position;
        position.x = Mathf.Clamp(position.x, minx, maxx);
        position.y = Mathf.Clamp(position.y, miny, maxy);
        transform.position = position;
    }
    
    /// <summary>
    ///
    ///it needs to truly handle the 8 sprite direction and the flipping if I am gonna include it, doubt it will work tho
    /// </summary>
    private void UpdateSpriteDirection()
    {
        if (spriteRenderer == null) return;
        if (movementInput.magnitude < 0.1f) return;
        
        if (use8DirectionalSprites)
        {

            float angle = Mathf.Atan2(movementInput.y, movementInput.x) * Mathf.Rad2Deg;

            ///
            /// 
            /// based on my input vector calculation above, should create a if condition to autoatically 
            /// reposition my angle if less than 0 to only around 0 to 360 degrees to prevent uneccesary conflicts
            if (angle < 0) angle += 360f;
            
            //if i had more time, i would have used the 45f degreees angles to map a 8 sprite direction
            
            // 0° = East
            // 45° = NorthEast
            // 90° = North (continue)
            currentDirection = Mathf.RoundToInt(angle / 45f) % 8;
            
            //this is optional really if it will with an animation but I doubt it, its only useful for still sprites which I changed anyways
            if (useHorizontalFlipping)
            {
                bool shouldFlip = (currentDirection == (int)Direction.West || 
                                  currentDirection == (int)Direction.NorthWest || 
                                  currentDirection == (int)Direction.SouthWest);
                
                spriteRenderer.flipX = shouldFlip;
                isFacingRight = !shouldFlip;
                
                //this just remaps the direction for the animator 
                if (shouldFlip)
                {
                    if (currentDirection == (int)Direction.West)
                        currentDirection = (int)Direction.East;
                    else if (currentDirection == (int)Direction.NorthWest)
                        currentDirection = (int)Direction.NorthEast;
                    else if (currentDirection == (int)Direction.SouthWest)
                        currentDirection = (int)Direction.SouthEast;
                }
            }
        }
        else
        {
            //if all goes to shit, go back to 4 direction
            if (movementInput.x > 0.1f && !isFacingRight)
            {
                FlipSprite(true);
            }
            else if (movementInput.x < -0.1f && isFacingRight)
            {
                FlipSprite(false);
            }
        }
    }
    
    /// <summary>
    ///testing, probably wont work cuz im animating in aseprite
    /// </summary>
    private void FlipSprite(bool faceRight)
    {
        isFacingRight = faceRight;
        spriteRenderer.flipX = !faceRight;
    }
    
    /// <summary>
    ///movement state updates parameters
    /// </summary>
    private void UpdateAnimationState()
    {
        if (spriteAnimator == null) return;

        float normalizedSpeed = currentVelocity.magnitude / movementSpeed;
        
        ///(controls Idle <-> Running transitions)
        /// my animator does use this bool to easily create a way to measure true or falses if character 
        /// is moving or not
        spriteAnimator.SetBool(isMovingParameterName, normalizedSpeed > 0.1f);

        //heard this is useful for blend trees
        spriteAnimator.SetFloat(speedParameterName, normalizedSpeed);
        
        //detects 0-7 for directions
        ///                  .
        ///                .   .
        ///               .       . 
        ///                  .  .
        ///                    .
        /// 
        /// best way i can represent it really
        if (use8DirectionalSprites)
        {
            spriteAnimator.SetInteger(directionParameterName, currentDirection);
        }
    }

    
    /// <summary>
    ///on death with player health, it stops all movement
    /// </summary>
    public void StopMovement()
    {
        movementInput = Vector2.zero;
        currentVelocity = Vector2.zero;
        rb2d.linearVelocity = Vector2.zero;
    }
    
   
    public Vector2 GetVelocity()
    {
        return currentVelocity;
    }
    
    
    public float GetNormalizedSpeed()
    {
        return currentVelocity.magnitude / movementSpeed;
    }
    
 
    public bool IsMoving()
    {
        return currentVelocity.magnitude > 0.1f;
    }
    

    public bool IsFacingRight()
    {
        return isFacingRight;
    }
    
 
    public int GetDirection()
    {
        return currentDirection;
    }
    

    
    private void OnDrawGizmosSelected()
    {
        //for reintroducing purposes for myself especially when explainin
//this draws a box....yeah.
        if (useBoundaries)
        {
            Gizmos.color = Color.yellow;
            Vector3 center = new Vector3((minx + maxx) / 2f, (miny + maxy) / 2f, 0f);
            Vector3 size = new Vector3(maxx - minx, maxy - miny, 0f);
            Gizmos.DrawWireCube(center, size);
        }
        
        //this drwas the velocity vector thin 
        if (Application.isPlaying && rb2d != null)
        {
            Gizmos.color = Color.green;
            Gizmos.DrawRay(transform.position, currentVelocity);
            
            //this draws the indicator if it sognna be used
            if (use8DirectionalSprites)
            {
                Gizmos.color = Color.cyan;
                Vector3 dirIndicator = Vector3.zero;
                
                //for visuals in scene view, it converts all the enums to visual vectors in up down and left and right arrows
                switch ((Direction)currentDirection)
                {
                    case Direction.East: dirIndicator = Vector3.right; break;
                    case Direction.NorthEast: dirIndicator = new Vector3(1, 1, 0).normalized; break;
                    case Direction.North: dirIndicator = Vector3.up; break;
                    case Direction.NorthWest: dirIndicator = new Vector3(-1, 1, 0).normalized; break;
                    case Direction.West: dirIndicator = Vector3.left; break;
                    case Direction.SouthWest: dirIndicator = new Vector3(-1, -1, 0).normalized; break;
                    case Direction.South: dirIndicator = Vector3.down; break;
                    case Direction.SouthEast: dirIndicator = new Vector3(1, -1, 0).normalized; break;
                }
                
                Gizmos.DrawRay(transform.position, dirIndicator * 1.5f);
            }
        }
    }
}