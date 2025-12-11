using UnityEngine;
using System.Collections;
using System;
using Unity.VisualScripting;

public class IsometricPlayerAttack: MonoBehaviour
{
    /// <summary>
    /// 
    /// no idea how this one is gonna go, but this will compensate for the script
    /// that enemyai uses which creates a health system for them. 
    /// 
    /// </summary>
    [Header("PLAYER | ATTACK DAMAGE CONFIGURATION")]
    ///
    /// 
    /// 
    /// 
    [SerializeField]
    [Tooltip("this is gonna be the overall base damage for each attack")]
    private int primaryAttackDmg = 10; 

    [SerializeField]
    [Tooltip("combo mutipler helps to scale the damage")]
    private bool utiliseComboMultiplier = false; 

    [SerializeField]
    [Tooltip("max combo multiplier, 1 should keep it as the default but 2 does double damage, definetely good for pick ups or upgrades")]
    private float maxComboMultiplier = 1.224f; 
    ///
    ///
    ///
    [Header("PLAYER | ATTACK TIMINGS")]
    ///
    /// 
    ///
    /// 
    [SerializeField]
    [Tooltip("attack cooldown in seconds")]
    private float attackCooldown = 0.567f; 

    [SerializeField]
    [Tooltip("this will handle animation curves ot attack animations in frames per second")]
    private float attackDuration = 0.35f; //calculate this for my animation later

    [SerializeField] 
    [Tooltip("i think this will help create the input buffer window for combo continuation, basically a combo window")]
    private float comboOpening=0.55f; 
    ///
    ///
    ///
    [Header("PLAYER | ATTACK DETECTION METHOD")]
    ///
    /// 
    /// 
    /// 
    [SerializeField]
    [Tooltip("detection method where raycase it for directional, adn overlap is for area")]
    private AttackWeaponType detectionMode = AttackWeaponType.BeetleBlades; //this is for the area of attack
    ///
    ///
    ///
    [Header("PLAYER | OVERLAP CIRCLE SETTINGS")]
    [SerializeField]
    [Tooltip("this is the range for the player center radius attack")]
    private float attackRange = 1.225f;

    [SerializeField]
    [Tooltip("creating an offeset to play around with from the players positiuon to help create an attack origin")]
    private Vector2 attackOffset = Vector2.zero;

    [SerializeField]
    [Tooltip("this creates the layermask for the enemy detectioning")]
    private LayerMask enemyLayer; 
    ///
    ///
    ///
    [Header("PLAYER | RAYCAST SETTINGS")]
    ///
    /// 
    /// 
    /// 
    [SerializeField]
    [Tooltip("this creates the distance for directional attacks;raycast")]
    private float raycastDist = 2f; 
    
    [SerializeField] 
    [Tooltip("raycast overall thickness, helps the feature before for chunkier area attacks")]
    private float raycastRadiusThickness = 0.9f;
    ///
    ///
    ///
    [Header("PLAYER | DIRECTIONAL ATTACK")]
    ///
    /// 
    /// 
    /// 
    [SerializeField]
    [Tooltip("attacking in either movement position or fixed direction")]
    private bool useMovementDir = true;
    
    [SerializeField]
    [Tooltip("this will be the fixed attack position basically")]
    private Vector2 stuckInPosAttackDir = Vector2.right; 
    ///
    ///
    ///
    [Header ("PLAYER | GONNA KNOCK YOU OUTTT (Yeah you guessed right its knockback configs)")]
    [SerializeField]
    [Tooltip("knockback force time!")]
    private bool ApplyKnockback = true; 

    [SerializeField]
    [Tooltip("almost forget to set a frickin magnitude")]
    private float knockbackForce = 10f;

    [SerializeField]
    [Tooltip("lmockback durations")]
    private float knockbackDuration = 0.29f;
    ///
    ///
    ///
    ///
    ///
    [Header("PLAYER | INPUT CONFIGS")]
    [SerializeField]
    [Tooltip("the key for attack, preferably space for primary")]
    private KeyCode attackKey = KeyCode.Space; 

    [SerializeField]
    [Tooltip("alternative use is now allowed")]
    private bool useMouseAttack = true; 

    [SerializeField]
    [Tooltip("need to first establish teh index of use of alternative attack button, 0 is left, right is 1, middle is 2")]
    private int mouseButton = 0;
    ///
    ///
    ///
    [Header("PLAYER | POINTER REFERENCE")]
    [SerializeField]
    [Tooltip("attack pointer script for getting mouse aim direction")]
    private pointer attackPointer;
    ///
    ///
    ///
    [Header ("PLAYER | VISUAL FEEDBACK")]
    [SerializeField]
    [Tooltip("Enables the help of the gizmo visuals in the normal scene view, very useful")]
    private bool showDebugGizmos = true;

    [SerializeField]
    [Tooltip("sprite renderer for attack visual effect")]
    private SpriteRenderer attackVisualEffect; 

    [SerializeField]
    [Tooltip("duration for the same feature before but in seconds")]
    //used wawit for seconds function with this on 
    private float visualEffectDuration = 0.4f; 
    ///
    ///
    ///
    [Header("PLAYER | ANIMATION SETTINGS")]
    ///
    /// 
    /// 
    /// 
    /// 
    [SerializeField]
    [Tooltip("should work as a animator component for my attack animation created on aseprite, ill assign a auto finde too")]
    private Animator playerAnimator;

    [SerializeField]
    [Tooltip("assigning now a trigger parameter for my attack in animator")]
    private string attackTriggerNamee = "Attack";
    ///
    ///
    ///
    [Header("PLAYER | AUDIO")]
    [SerializeField]
    [Tooltip("audio source for attack sounds")]
    private AudioSource attackAudioSrc;
    
    [SerializeField]
    [Tooltip("swing/whoosh sounds when attacking, can have variations for different combos")]
    private AudioClip[] attackSwingSfx;
    
    [SerializeField]
    [Tooltip("impact sounds when hitting enemies, variations prevent repetition")]
    private AudioClip[] hitImpactSfx;
    
    [SerializeField]
    [Tooltip("miss sounds when attack doesnt hit anything")]
    private AudioClip[] missSfx;
    
    [SerializeField]
    [Tooltip("special sound for combo finishers or high combo counts")]
    private AudioClip[] comboFinisherSfx;
    
    [SerializeField]
    [Tooltip("pitch variation for swing sounds")]
    private float swingPitchVar = 0.1f;
    
    [SerializeField]
    [Tooltip("pitch variation for hit sounds")]
    private float hitPitchVar = 0.15f;
    
    [SerializeField]
    [Tooltip("volume for attack sounds")]
    private float attackVol = 0.7f;
    
    [SerializeField]
    [Tooltip("combo count that triggers special finisher sound")]
    private int comboFinisherThreshold = 5;
    
    /// <summary>
    /// 
    /// 
    /// now the use of the statetracking again
    /// keeping track of attack state, cooldowns, combo counters and direction
    /// 
    /// 
    /// </summary>
    private bool isAttacking = false;
    private bool canAttack = true; 
    private float lastAttackTime = 0f; 
    private int currentCombo = 0;
    private float lastComboTime=0f;
    private Rigidbody2D playerRigidbody; 
    private Vector2 lastMovementDir = Vector2.right; 

    /// <summary>
    /// 
    /// 
    /// had to learn enumerations again for weapon types
    /// basically different attack detection methods that can swap between
    /// 
    /// 
    /// </summary>
    public enum AttackWeaponType
    {
        BearFists, // area based detection utilising overcicle secretely
        BeetleBlades, //line detection (directional) utilises raycast.
        DoubleBeetleBlade  //rectangular detection this time, useful to make it seem like theres double the force kind of
    }

    void Start()
    {
        //so i still dont understand what cache means in the unity sense, but we
        //can use it to cache the rigidbody reference for moving alongside in direcitonal tracking
        playerRigidbody = GetComponent<Rigidbody2D>(); 
        if (playerRigidbody == null)
        {
            Debug.LogWarning("[player attack] no rigidbody2d found - cant track movement direction!");
        }

        if (enemyLayer == 0)
        {
            Debug.LogWarning("[player attack] enemy layer mask hasnt been properly set, may not be able to detect the enemies");
        }

        if (attackVisualEffect != null)
        {
            attackVisualEffect.enabled = false; 
        }

        if (playerAnimator == null)
        {
            playerAnimator = GetComponent<Animator>(); 
            if (playerAnimator == null)
            {
                Debug.LogWarning("[player attack] the attack animations disabled cuz no animator was found");
            } 
            else
            {
                Debug.Log("[player attack] animator has been found yipeeee progress! (oh and cached)");
            }
        }
        
        // auto finding the attack pointer if not assigned
        if (attackPointer == null)
        {
            attackPointer = GetComponentInChildren<pointer>();
            if (attackPointer != null)
            {
                Debug.Log("[player attack] found attack pointer in children yay");
            }
        }
        
        // initializing audio source
        InitializeAudio();
    }
    
    /// <summary>
    /// 
    /// setting up audio source if i forgot to assign it
    /// creating it programmatically if needed
    /// 
    /// </summary>
    private void InitializeAudio()
    {
        if (attackAudioSrc == null)
        {
            attackAudioSrc = GetComponent<AudioSource>();
            if (attackAudioSrc == null)
            {
                attackAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        attackAudioSrc.playOnAwake = false;
        attackAudioSrc.spatialBlend = 0f; //2D sound for isometric
        attackAudioSrc.volume = attackVol;
        
        Debug.Log("[player attack] audio initialized");
    }
    
    void Update()
    {
        // tracking movement direction for directional attacks
        if (playerRigidbody != null && useMovementDir)
        {
            Vector2 velocity = playerRigidbody.linearVelocity;
            if (velocity.magnitude > 0.1f)
            {
                lastMovementDir = velocity.normalized;
            }
            ///
            /// this helps to track movement direction for directional attacks
            /// the rest of the stuff beforehand was to create a cache rigidbody reference, then
            /// validate the layer mask configs that we setted up earlier on, and then hide the visual effect attacks first
            /// before applying any sort of flash effect
        }

        // combo timeout check
        if (Time.time - lastComboTime > comboOpening && currentCombo > 0)
        {
            comboResetterCallback();
        }

        // handling attack input now 
        if (allowingAttackInput())
        {
            ExectueAttack();
        }
    }
    
    /// <summary>
    /// 
    /// checking attack input based on selected mode
    /// if useMouseAttack is enabled, ONLY mouse clicks trigger attacks
    /// keyboard is fallback for testing or if mouse is disabled
    /// this keeps movement (WASD) and attack (mouse) completely separate
    /// 
    /// </summary>
    private bool allowingAttackInput()
    {
        bool inputDetected = false;
        
        // if using mouse attack mode, ONLY check mouse
        if (useMouseAttack)
        {
            inputDetected = Input.GetMouseButtonDown(mouseButton);
            
            // DEBUG: log mouse clicks
            if (inputDetected)
            {
                Debug.Log($"[player attack] MOUSE CLICK detected | canAttack: {canAttack} | isAttacking: {isAttacking}");
            }
        }
        // if mouse attack is disabled, fall back to keyboard
        else
        {
            inputDetected = Input.GetKeyDown(attackKey);
            
            // DEBUG: log keyboard presses
            if (inputDetected)
            {
                Debug.Log($"[player attack] KEYBOARD pressed | canAttack: {canAttack} | isAttacking: {isAttacking}");
            }
        }
        
        // early exit if no input detected
        if (!inputDetected)
        {
            return false;
        }
        
        // validate attack state (cooldown and animation checks)
        if (!canAttack || isAttacking)
        {
            Debug.Log("[player attack] cant attack right now - still in cooldown or animation");
            return false;
        }
        
        return true;
    }

    private void ExectueAttack()
    {
        isAttacking = true;
        canAttack = false; 
        lastAttackTime = Time.time; 

        // increment combo counter
        currentCombo++; 
        lastComboTime = Time.time; 
        
        // if using pointer for attack direction instead of movement
        if (attackPointer != null && !useMovementDir)
        {
            lastMovementDir = attackPointer.GetPointerDirection();
            Debug.Log($"[player attack] using pointer direction: {lastMovementDir}");
        }
        
        // playing attack swing sound
        PlayAttackSwingSound();
        
        // trigger attack animation
        if (playerAnimator != null && !string.IsNullOrEmpty(attackTriggerNamee))
        {
            playerAnimator.SetTrigger(attackTriggerNamee);
            Debug.Log($"[player attack] triggered '{attackTriggerNamee}' animation");
        }

        Debug.Log($"[player attack] executing attack {currentCombo} with {primaryAttackDmg} amount of damage!");

        // detect enemies based on current detection mode
        Collider2D[] hitEnemies = DetectEnemies();

        // damage all detected enemies
        if (hitEnemies != null && hitEnemies.Length > 0)
        {
            // playing hit impact sound
            PlayHitImpactSound();
            
            // check if this is a combo finisher
            if (currentCombo >= comboFinisherThreshold)
            {
                PlayComboFinisherSound();
            }
            
            foreach (Collider2D enemyCollider in hitEnemies)
            {
                LetEnemyTakeDamage(enemyCollider.gameObject);
            }

            Debug.Log($"[player attack] hit {hitEnemies.Length} enemies");
        }
        else
        {
            // playing miss sound when no enemies hit
            PlayMissSound();
            Debug.Log($"[player attack] no enemies have been hit");
        }

        // trigger visual feedback
        if (attackVisualEffect != null)
        {
            StartCoroutine(displayAttackVisusal());
        }

        StartCoroutine(AttackCooldownRoutine());
    }
    
    /// <summary>
    /// 
    /// playing attack swing/whoosh sound
    /// pitch varies based on combo count for escalation feeling
    /// 
    /// </summary>
    private void PlayAttackSwingSound()
    {
        if (attackAudioSrc == null || attackSwingSfx == null || attackSwingSfx.Length == 0) return;
        
        AudioClip selectedClip = attackSwingSfx[UnityEngine.Random.Range(0, attackSwingSfx.Length)];
        
        // slightly increase pitch with combo count for intensity
        float comboPitchBoost = Mathf.Min(currentCombo * 0.05f, 0.3f);
        attackAudioSrc.pitch = 1f + comboPitchBoost + UnityEngine.Random.Range(-swingPitchVar, swingPitchVar);
        
        attackAudioSrc.PlayOneShot(selectedClip);
    }
    
    /// <summary>
    /// 
    /// playing impact sound when hitting enemies
    /// uses pitch variation to prevent repetition
    /// 
    /// </summary>
    private void PlayHitImpactSound()
    {
        if (attackAudioSrc == null || hitImpactSfx == null || hitImpactSfx.Length == 0) return;
        
        AudioClip selectedClip = hitImpactSfx[UnityEngine.Random.Range(0, hitImpactSfx.Length)];
        attackAudioSrc.pitch = 1f + UnityEngine.Random.Range(-hitPitchVar, hitPitchVar);
        attackAudioSrc.PlayOneShot(selectedClip);
    }
    
    /// <summary>
    /// 
    /// playing miss sound when attack doesnt hit anything
    /// optional - can leave array empty if dont want miss sounds
    /// 
    /// </summary>
    private void PlayMissSound()
    {
        if (attackAudioSrc == null || missSfx == null || missSfx.Length == 0) return;
        
        AudioClip selectedClip = missSfx[UnityEngine.Random.Range(0, missSfx.Length)];
        attackAudioSrc.pitch = 1f + UnityEngine.Random.Range(-0.1f, 0.1f);
        attackAudioSrc.PlayOneShot(selectedClip, 0.5f); // quieter than hits
    }
    
    /// <summary>
    /// 
    /// playing special combo finisher sound
    /// triggers when reaching combo threshold
    /// 
    /// </summary>
    private void PlayComboFinisherSound()
    {
        if (attackAudioSrc == null || comboFinisherSfx == null || comboFinisherSfx.Length == 0) return;
        
        AudioClip selectedClip = comboFinisherSfx[UnityEngine.Random.Range(0, comboFinisherSfx.Length)];
        attackAudioSrc.pitch = 1f;
        attackAudioSrc.PlayOneShot(selectedClip, 1.2f); // louder for emphasis
        
        Debug.Log($"[player attack] COMBO FINISHER! {currentCombo} hits!");
    }
    private Collider2D[] DetectEnemies()
    {
        //using pointer position instead of player position for click-based attacks
        Vector2 attackPosition;
        
        if (attackPointer != null)
        {
            //attack originates from pointer (mouse cursor position)
            attackPosition = attackPointer.transform.position;
        }
        else
        {
            //fallback to player position if pointer missing
            attackPosition = (Vector2)transform.position + attackOffset;
            Debug.LogWarning("[player attack] no pointer reference, using player position");
        }
        
        switch (detectionMode)
        {
            case AttackWeaponType.BearFists:
                return Physics2D.OverlapCircleAll(attackPosition, attackRange, enemyLayer);
            
            case AttackWeaponType.BeetleBlades:
                //for beetle blades, use pointer direction instead of movement
                Vector2 direction = attackPointer != null ? attackPointer.GetPointerDirection() : 
                                   (useMovementDir ? lastMovementDir : stuckInPosAttackDir);
                RaycastHit2D[] rayHits = Physics2D.CircleCastAll(
                    attackPosition, raycastRadiusThickness, direction, raycastDist, enemyLayer); 
                
                //now to just convert all of the raycasthit2d array to the collider 2d array
                Collider2D[] colliders = new Collider2D[rayHits.Length];
                for (int i = 0; i < rayHits.Length; i++)
                {
                    colliders[i] = rayHits[i].collider; 
                }
                return colliders;
            
            case AttackWeaponType.DoubleBeetleBlade:
                Vector2 boxSize = new Vector2(attackRange * 2f, attackRange); //i could adjust this for later
                return Physics2D.OverlapBoxAll(attackPosition, boxSize, 0f, enemyLayer);

            default:
                return new Collider2D[0];
        }
    }

    /// <summary>
    /// 
    /// calculate final damage with combo multiplier if enabled
    /// then apply damage to enemy through their ai component
    /// 
    /// </summary>
    private void LetEnemyTakeDamage(GameObject enemy)
    {
        //calculate the final damage with the combo multiplier
        int finalDamage = primaryAttackDmg; 
        if (utiliseComboMultiplier)
        {
            float multiplier = Mathf.Lerp(1f, maxComboMultiplier, (currentCombo - 1) / 5f); //coming back to augment this if i want bigger values for the boss
            finalDamage = Mathf.RoundToInt(primaryAttackDmg * multiplier); 
        }

        //check if its a boss first
        isoBossController bossController = enemy.GetComponent<isoBossController>();
        if (bossController != null)
        {
            bossController.TakeDamage(finalDamage);
            Debug.Log($"[player attack] dealt {finalDamage} damage to boss {enemy.name}");
            
            if (ApplyKnockback)
            {
                ApplyKnockBackToEnemy(enemy);
            }
            return;
        }

        //find the isometric enemy ai component  
        Isometricenemyai enemyAI = enemy.GetComponent<Isometricenemyai>(); //is it best to do this for the spawner or enemies?...hmmm

        if (enemyAI != null)
        {
            enemyAI.TakeDamage(finalDamage); //need to now create the take damage component and map it over to the final damage
            Debug.Log($"[player attack] dealt {finalDamage} damage to {enemy.name}");
        }
        else
        {
            //fallback check for health component interface later
            Debug.LogWarning($"[player attack] {enemy.name} is missing an isometric enemy ai component or boss controller");
        }

        if (ApplyKnockback)
        {
            ApplyKnockBackToEnemy(enemy);
        }
    }

    /// <summary>
    /// 
    /// first time learning knockback components from player to enemy
    /// calculate direction from player to enemy then apply impulse force
    /// 
    /// </summary>
    private void ApplyKnockBackToEnemy(GameObject enemy)
    {
        Rigidbody2D enemyRb = enemy.GetComponent<Rigidbody2D>(); 
        if (enemyRb == null) return; 

        // calculate knockback direction (away from player)
        Vector2 knockbackDir = ((Vector2)enemy.transform.position - (Vector2)transform.position).normalized; 

        // application now of impulsive force 
        enemyRb.AddForce(knockbackDir * knockbackForce, ForceMode2D.Impulse); 
        // this adds an impulse which is a crucial element for my game in terms of
        // having space for your character to move and not be huddled up in one area
        // gives a chance for escape

        Debug.Log($"[player attack] applied knockback to {enemy.name}");
    }

    /// <summary>
    /// 
    /// coroutine for attack cooldown timing
    /// waits for attack duration then sets isAttacking false
    /// then waits remaining cooldown time before allowing next attack
    /// 
    /// </summary>
    private IEnumerator AttackCooldownRoutine()
    {
        yield return new WaitForSeconds(attackDuration);
        isAttacking = false;

        yield return new WaitForSeconds(attackCooldown - attackDuration);
        canAttack = true;
    }

    /// <summary>
    /// 
    /// displays attack visual effect sprite at attack position
    /// waits for duration then hides it again
    /// 
    /// </summary>
    private IEnumerator displayAttackVisusal()
    {
        attackVisualEffect.enabled = true; 

        // position at attack location
        Vector2 attackposition = (Vector2)transform.position + attackOffset; 
        attackVisualEffect.transform.position = attackposition; 

        yield return new WaitForSeconds(visualEffectDuration); 

        attackVisualEffect.enabled = false;
    }

    /// <summary>
    /// 
    /// resets combo counter back to zero
    /// 
    /// </summary>
    private void comboResetterCallback()
    {
        currentCombo = 0; 
        Debug.Log("[player attack] combo has been officially reset!");
    }

    // public methods for external scripts to modify attack parameters
    public void setAttackDmg(int newDamage)
    {
        primaryAttackDmg = Mathf.Max(1, newDamage);
    }

    public void AttackRange(float newRange)
    {
        attackRange = Mathf.Max(0.25f, newRange); 
    }

    public int stackCurrentCombo() => currentCombo; 

    public bool IsCurrentlyAttacking() => isAttacking;

    ///
    /// 
    /// 
    /// some anxiety reducing space here...
    /// 
    /// 
    /// 
    /// 
    public void attackTrigger()
    {
        if (canAttack && !isAttacking)
        {
            ExectueAttack();
        }
    }

    ///
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    /// the gizmosssssss
    /// drawing attack range visualization in scene view
    /// changes color based on if currently attacking or not
    void OnDrawGizmosSelected()
    {
        if (!showDebugGizmos) return; 
        
        //using pointer position if available, otherwise fallback to player position
        Vector2 attackPosition;
        if (Application.isPlaying && attackPointer != null)
        {
            attackPosition = attackPointer.transform.position;
        }
        else
        {
            attackPosition = (Vector2)transform.position + attackOffset;
        }

        //detection mode drawings for the weapon types (weapontypes are a name identifier for the kinds of detection modes to 
        //swap around with, and i hope the idea works to kind of create weapon types easier that way

        switch (detectionMode)
        {
            case AttackWeaponType.BearFists:
                Gizmos.color = isAttacking ? Color.red : Color.yellow; 
                Gizmos.DrawWireSphere(attackPosition, attackRange);
                break;

            case AttackWeaponType.BeetleBlades:
                Gizmos.color = isAttacking ? Color.red : Color.cyan;
                Vector2 dir;
                if (Application.isPlaying && attackPointer != null)
                {
                    dir = attackPointer.GetPointerDirection();
                }
                else
                {
                    dir = useMovementDir ? lastMovementDir : stuckInPosAttackDir;
                }
                Vector2 endpoint = attackPosition + dir * raycastDist;

                //gonna now draw the next lines for the raycast to be completed is a visual feedback 
                //for my game to easily identify amongst level changes
                Gizmos.DrawLine(attackPosition, endpoint);

                Gizmos.DrawWireSphere(attackPosition, raycastRadiusThickness); 
                Gizmos.DrawWireSphere(endpoint, raycastRadiusThickness);
                break; 

            case AttackWeaponType.DoubleBeetleBlade: 
                Gizmos.color = isAttacking ? Color.red : Color.green;
                Vector2 boxSize = new Vector2(attackRange * 2f, attackRange); 
                Gizmos.DrawWireCube(attackPosition, boxSize);
                break;
        }

        //draw line from player to attack position
        Gizmos.color = Color.white; 
        Gizmos.DrawLine(transform.position, attackPosition); 
    }
}