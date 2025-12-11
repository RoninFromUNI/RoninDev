using UnityEngine;
using System.Collections;
using System;
using Random = UnityEngine.Random;

/// <summary> 
/// So far the script is aimed to practice in creating variation with a 2d isometric enemy with chase and attack
/// it is deemed to be maybe compatible until further testing with Isometricshcharactercontroller1
/// </summary>

[RequireComponent(typeof(Rigidbody2D))]
[RequireComponent(typeof(SpriteRenderer))]
public class Isometricenemyai : MonoBehaviour
{
    [Header("target settings")]
    [SerializeField]
    [Tooltip("direct who enemy should chase, auto locates if target not set")]
    private Transform playerTarget;

    [SerializeField]
    [Tooltip("this basically tags to find the player if not manually assingned")]
    private string playerTag = "Player";  //CHANGE TAG HERE IF TAG IS CUSTOMISED
    ///
    ///
    ///
    [Header ("ENEMY | HEALTH CONFIGS")]
    [SerializeField]
    [Tooltip("this will just give maximum health points")]
    private int maxHP = 100;

    [SerializeField]
    [Tooltip("hp value at the current state")]
    private int currentHP;

    [SerializeField]
    [Tooltip("this will give a brief moment of invincibilithy for the enemies after taking damage to not allow spam attacks")]
    private float invulnerabilityDuration=0.4f; //makes game more fun and last longer

    [SerializeField]
    [Tooltip("game object total annihalation muhahahah")]
    private bool destroyOnDeath = true;

    [SerializeField]
    [Tooltip("delay for the death for enemy")]
    private float enemydeathDelay = 0.855f;

    //some tracking 
    private bool isInvulnerable = false; 
    private bool isDead = false; 
    ///
    ///
    ///
    [Header("ENEMY | movement settings")]
    [SerializeField]
    [Tooltip("Enemy movement speed adjustment")]
    private float speedAdjust = 3.5f;

    [SerializeField]
    [Tooltip("keep an eye on the vertical compression for the iso-movement for player, it must match")]
    private float isometricYScale = 0.5f;

    [SerializeField]
    [Tooltip("movement smoothing/dampening")]
    private float smoothMovementAdjust = 0.85f;
    ///
    ///
    ///
    [Header("ENEMY | Chase Behaviour")]
    [SerializeField]
    [Tooltip("enemy range cirlce for when it should start chasing enemy")]
    private float detectionRange = 10f;

    [SerializeField]
    [Tooltip("Distance at which enemy stops moving (attack range)")]
    private float attackRange = 2.75f;

    [SerializeField]
    [Tooltip("minimum setting distance to maintain away from the player")]
    private float stopDist = 0.25f;
    ///
    ///
    ///
    [Header("ENEMY | ATTACK")]
    [SerializeField]
    [Tooltip("timing of attacks persecond")]
    private float attackCooling = 1.75f; //need to adjust for game pacing

    [SerializeField]
    [Tooltip("dmg per attack")]
    private int attackDmg = 8;

    [SerializeField]
    [Tooltip("attack behavior toggle (true/false boolean)")] //very good for intro
    //can utilise for dummy attack behavior for tutorial
    private bool canAttack = true;
    ///
    ///
    ///
    [Header("ENEMY | SPRITE")]
    [SerializeField]
    [Tooltip("8-directional sprite activate true or false for use")]
    private bool use8Sprites = true;

    [SerializeField]
    [Tooltip("flipping sprite cuz too less time to do more asset anims")]
    private bool mirrorSpriteMovement = true;
    ///
    ///
    ///
    [Header("ENEMY | ANIMATION")]
    [SerializeField]
    private Animator enemyAnimator;

    [SerializeField]
    private string velocityParameterName = "velocity";

    [SerializeField]
    private string directionParameterName = "Direction";

    [SerializeField]
    private string isMovingParameterName = "IsMoving";

    [SerializeField]
    private string attackTriggerName = "Attack";
    ///
    ///
    ///
    [Header("ENEMY | AUDIO")]
    [SerializeField]
    [Tooltip("audio source for enemy sounds")]
    private AudioSource enemyAudioSrc;
    
    [SerializeField]
    [Tooltip("sounds when enemy takes damage, can have variations")]
    private AudioClip[] damageSfx;
    
    [SerializeField]
    [Tooltip("sounds when enemy dies")]
    private AudioClip[] deathSfx;
    
    [SerializeField]
    [Tooltip("sounds when enemy attacks player")]
    private AudioClip[] attackSfx;
    
    [SerializeField]
    [Tooltip("pitch variation for damage sounds to prevent repetition")]
    private float damagePitchVar = 0.15f;
    
    [SerializeField]
    [Tooltip("pitch variation for attack sounds")]
    private float attackPitchVar = 0.1f;
    
    [SerializeField]
    [Tooltip("volume for enemy sounds")]
    private float enemyVol = 0.6f;

    /// <summary>
    /// 
    /// 
    /// private component references and state tracking
    /// 
    /// 
    /// </summary>
    private Rigidbody2D rb2d;
    private SpriteRenderer spriteRenderer;

    //creating the states now on default for the enemy until conditions are met
    private Vector2 currentVelocity;
    private int currentDirection = 0;
    private float lastAttackTime = -1000f;
    private bool isPlayerInRange = false;
    private bool isPlayerInAttackRange = false;

    public event Action OnDeath; 
    public static event Action<GameObject> OnAnyEnemyDeath;

    void Start()
    {
        currentHP = maxHP; 
        rb2d = GetComponent<Rigidbody2D>();
        spriteRenderer = GetComponent<SpriteRenderer>();
        //component caching, need to understand why i need to do this again

        //physics here
        rb2d.gravityScale = 0f;
        rb2d.freezeRotation = true;
        rb2d.collisionDetectionMode = CollisionDetectionMode2D.Continuous;

        //locate animator automatically
        if (enemyAnimator == null)
        {
            enemyAnimator = GetComponent<Animator>();
        }
        
        //initializing audio source
        InitializeAudio();

        //locating enemy automatically too here
        if (playerTarget == null)
        {
            GameObject playerObj = GameObject.FindGameObjectWithTag(playerTag); //migut delete this if it doesnt work. 
            if (playerObj != null)
            {
                playerTarget = playerObj.transform;
                //creating a log to know its working

                Debug.Log($"[{gameObject.name}]  level 1 enemy found dwayne: {playerObj.name}");
            }

            else
            {
                Debug.LogWarning($"{gameObject.name}] oh no we cant find dwayne '{playerTag}'. correct the damn tag!");
            }
        }
    }
    
    /// <summary>
    /// 
    /// setting up audio source if i forgot to assign it
    /// creating it programmatically if needed
    /// 
    /// </summary>
    private void InitializeAudio()
    {
        if (enemyAudioSrc == null)
        {
            enemyAudioSrc = GetComponent<AudioSource>();
            if (enemyAudioSrc == null)
            {
                enemyAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        enemyAudioSrc.playOnAwake = false;
        enemyAudioSrc.spatialBlend = 0f; //2D sound for isometric
        enemyAudioSrc.volume = enemyVol;
    }

    void Update() //i should as well learn the importance of start and upate, but main guess is one sets the defaults and one progresively updates as we go alopng the game
    {
        if (playerTarget == null) return;
        float distanceToPlayer = Vector2.Distance(transform.position, playerTarget.position); //maybe should consider player roration too.

        //update range stats logic
        isPlayerInRange = distanceToPlayer <= detectionRange;
        isPlayerInAttackRange = distanceToPlayer <= attackRange;

        if (isPlayerInRange)
        {
            UpdateSpriteDirection();
        }
        if (canAttack && isPlayerInAttackRange && Time.time >= lastAttackTime + attackCooling)
        {
            DoAttack();
        }

        //then i need to update animation state

        UpdateAnimationState();
    }

    void FixedUpdate()
    {
        if (playerTarget == null) return;
        ApplyMovement();
    }
    
    void ApplyMovement()
    {
        //only happens if player is in range but not too close
        float distanceToPlayer = Vector2.Distance(transform.position, playerTarget.position);
        if (isPlayerInRange && distanceToPlayer > stopDist)
        {
            //direction calculation towards dwayne the player
            Vector2 directionToPlayer = ((Vector2)playerTarget.position - (Vector2)transform.position).normalized;

            //isometric space is where the player would be transfomred too
            Vector2 isometricMovement = TransformToIsometric2D(directionToPlayer);
            isometricMovement.Normalize();

            //calculating target velcouty here noo

            Vector2 targetVelocity = isometricMovement * speedAdjust;


            //smoothing programming application

            currentVelocity = Vector2.Lerp(currentVelocity, targetVelocity, 1f - smoothMovementAdjust);
        }
        else
        {
            //we need him to now stop moving, codinghere i think vcan be applied to level 2 enemy
            currentVelocity = Vector2.Lerp(currentVelocity, Vector2.zero, 1f - smoothMovementAdjust);
        }
        //velocity now needs to be applied here for some reason 
        rb2d.linearVelocity = currentVelocity;
    }

    Vector2 TransformToIsometric2D(Vector2 input)
    {
        float isoX = input.x - input.y;
        float isoY = (input.x + input.y) * isometricYScale;
        return new Vector2(isoX, isoY);
    }

    void UpdateSpriteDirection()
    {
        if (playerTarget == null) return;
        //direction vector needs to be calculated near to player
        Vector2 directionToPlayer = (Vector2)playerTarget.position - (Vector2)transform.position;

        if (directionToPlayer.magnitude < 0.1f) return;
        float angle = Mathf.Atan2(directionToPlayer.y, directionToPlayer.x) * Mathf.Rad2Deg;
        if (angle < 0) angle += 360f;
        //this will help make the angle variable exist finally and can do the rest of the codin!

        //attempting to calculate the angle before mapping to direciton 
        if (use8Sprites)
        {
            currentDirection = Mathf.RoundToInt(angle / 45f) % 8;
        }
        else
        {
            currentDirection = Mathf.RoundToInt(angle / 90f) % 4;
            currentDirection *= 2;
        }

        //give em the bird, just kidding flipping the sprite now
        if (mirrorSpriteMovement)
        {
            bool ShouldFlip = (currentDirection == 6 || currentDirection == 5 || currentDirection == 7);
            spriteRenderer.flipX = ShouldFlip;
            if (ShouldFlip)
            {
                if (currentDirection == 6) currentDirection = 2; // west to east hopefully
                else if (currentDirection == 5) currentDirection = 3; //northwest hopefully to northeast
                else if (currentDirection == 7) currentDirection = 1; //so that handles soutwest to southeast now finally
            }
        }
    }

    void UpdateAnimationState()
    {
        if (enemyAnimator == null) return;

        float normalisedVelocity = currentVelocity.magnitude / speedAdjust;

        if (!string.IsNullOrEmpty(velocityParameterName))
        {
            enemyAnimator.SetFloat(velocityParameterName, normalisedVelocity);
        }
        if (!string.IsNullOrEmpty(directionParameterName))
        {
            enemyAnimator.SetInteger(directionParameterName, currentDirection);
        }
        if (!string.IsNullOrEmpty(isMovingParameterName))
        {
            enemyAnimator.SetBool(isMovingParameterName, normalisedVelocity > 0.1f);
        }
    }

    /// <summary>
    /// 
    /// enemy attacks player
    /// playing attack animation and sound
    /// dealing damage to player health
    /// 
    /// </summary>
    void DoAttack()
    {
        lastAttackTime = Time.time;

        //animation trigger 
        if (enemyAnimator != null && !string.IsNullOrEmpty(attackTriggerName))
        {
            enemyAnimator.SetTrigger(attackTriggerName);
        }
        
        //playing attack sound
        PlayAttackSound();

        //health player system is gonna be associated with this one, can customise this freely
        //very damagin to player

        if (playerTarget != null)
        {
            var playerHealth = playerTarget.GetComponent<playerhealth_Dwayne>(); //simplified and now able to call player health dwayne as a variable to my code logic
            if (playerHealth != null)
            {
                playerHealth.TakeDamage(attackDmg); //this needs the player health to be working as well alongside this in the same place
            }
            Debug.Log($"{gameObject.name}] Attacked dwayne for {attackDmg} damage, ouch!");
        }
    }
    
    /// <summary>
    /// 
    /// playing attack sound with pitch variation
    /// prevents repetitive audio when multiple enemies attack
    /// 
    /// </summary>
    private void PlayAttackSound()
    {
        if (enemyAudioSrc == null || attackSfx == null || attackSfx.Length == 0) return;
        
        AudioClip selectedClip = attackSfx[Random.Range(0, attackSfx.Length)];
        enemyAudioSrc.pitch = 1f + Random.Range(-attackPitchVar, attackPitchVar);
        enemyAudioSrc.PlayOneShot(selectedClip);
    }
    
    /// <summary>
    /// 
    /// playing damage sound with pitch variation
    /// called when enemy takes damage
    /// 
    /// </summary>
    private void PlayDamageSound()
    {
        if (enemyAudioSrc == null || damageSfx == null || damageSfx.Length == 0) return;
        
        AudioClip selectedClip = damageSfx[Random.Range(0, damageSfx.Length)];
        enemyAudioSrc.pitch = 1f + Random.Range(-damagePitchVar, damagePitchVar);
        enemyAudioSrc.PlayOneShot(selectedClip);
    }
    
    /// <summary>
    /// 
    /// playing death sound when enemy dies
    /// 
    /// </summary>
    private void PlayDeathSound()
    {
        if (enemyAudioSrc == null || deathSfx == null || deathSfx.Length == 0) return;
        
        AudioClip selectedClip = deathSfx[Random.Range(0, deathSfx.Length)];
        enemyAudioSrc.pitch = 1f + Random.Range(-0.1f, 0.1f);
        enemyAudioSrc.PlayOneShot(selectedClip);
    }

    /// <summary>
    /// 
    /// death sequence
    /// triggering death events, disabling components
    /// playing death sound and destroying gameobject
    /// 
    /// </summary>
    void Die()
    {
        if (isDead) return; 

        isDead = true; 
        Debug.Log($"[enemy] {gameObject.name} died"); 

        //logging my days of progress
        //23rd of november i found the reason why the condition for killing 5 enemies was missing

        OnDeath?.Invoke(); 
        OnAnyEnemyDeath?.Invoke(gameObject); 
        Debug.Log($"[enemy] death event has finally been triggered for {gameObject.name} HAHA YOU DEAD...well the enemy is");
        
        //playing death sound
        PlayDeathSound();

        //completely disable everything
        enabled = false; 
        Rigidbody2D rb = GetComponent<Rigidbody2D>();
        if(rb!=null)
        {
            rb.linearVelocity = Vector2.zero; //side note, if vectors are appearing with a red underline, check the packages for system numetrics and delete it
        }

        if(destroyOnDeath)
        {
            Destroy(gameObject,enemydeathDelay); //basically we are giving permission for our game object to die.
        }
    }

    //could link this debug log to maybe a display hover damage counter if time is left

    //need to now set some external control methods on public

    public void heal (int healAmount)
    {
        if (isDead) return; 
        currentHP = Mathf.Min(currentHP + healAmount, maxHP);
        Debug.Log($"[enemy] {gameObject.name} healed {healAmount} - health: {currentHP}/{maxHP}");
    }

    public void immediateDeath()
    {
        currentHP = 0; 
        Die(); 
    }

    private IEnumerator InvulnerabilityRoutine()
    {
        isInvulnerable = true; 
        //might add a flash sprite her4

        yield return new WaitForSeconds(invulnerabilityDuration);
        isInvulnerable =false;
    }
    
    /// <summary>
    /// 
    /// taking damage from player attacks
    /// playing damage sound and checking if dead
    /// 
    /// </summary>
    public void TakeDamage(int damageAmount)
    {
        //need to now set up my if dead or invulnerable stages0
        if (isDead || isInvulnerable)
        {
            return;
        }

        currentHP -=  damageAmount;
        Debug.Log($"[enemy] {gameObject.name} took {damageAmount} damage - health {currentHP}/{maxHP}");
        
        //playing damage sound
        PlayDamageSound();

        //looking into frames utilising coroutines now 
        StartCoroutine(InvulnerabilityRoutine());

        if (currentHP<=0)
        {
            Die();
        }
    }

    public int GetCurrentHealth() => currentHP;
    public int getMaxHP() => maxHP;
    public bool IsDead() => isDead;
    public float GetHealthPercentage()=>(float) currentHP/maxHP;
    
    public void SetTarget(Transform newTarget)
    {
        playerTarget = newTarget;
    }
    
    public void SetMoveSpeed(float newSpeed)
    {
        speedAdjust = Mathf.Max(0f, newSpeed);
    }

    public bool IsInAttackRange()
    {
        return isPlayerInAttackRange;
    }
    
    public bool IsInDetectionRange()
    {
        return isPlayerInRange;
    }

    void OnDrawGizmosSelected()
    {
        //adding a litle detectionr ange colour
        Gizmos.color = Color.yellow;
        Gizmos.DrawWireSphere(transform.position, detectionRange);

        //range for attack now 
        Gizmos.color = Color.red;
        Gizmos.DrawWireSphere(transform.position, stopDist);
        // now a line to the player, not sure if gizmos are yet needed but lets go with it
        if (playerTarget != null)
        {
            Gizmos.color = isPlayerInAttackRange ? Color.red : (isPlayerInRange ? Color.yellow : Color.gray);
            Gizmos.DrawLine(transform.position, playerTarget.position);
        }
    }
}


/// instructions made by etherealronin/riyaz ismail 2215624
/// 
/// enemy variation can be setup through this 
/// 
/// so first i just need to add this cript to my game object and it will automatically add a rb2d and sprite renderer if missing
/// tag the main player as player  ot change the playertag field entiely
/// and it will automatically find the player on teh start
/// 
/// there are different ranges of detection, attack distance and stop
/// 
/// 10 is detection range, 1.5 is close to attack but i may have adjusted it for my preference, stop distance is set to 1 to not come to close and give running space
/// 
/// 
/// i have created a optional player health sysem