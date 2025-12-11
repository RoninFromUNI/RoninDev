using UnityEngine;
using System.Collections;


/// <summary>
/// 
///stationary boss controller for cyberclone
///spawns enemies and opens up for damage after player kills enough minions
///phases include like closed (invulnerable), opening, vulnerable, closing, defeated
/// 
///had to learn state machines for this one, basically tracking what phase the boss is in
/// 
/// </summary>

public class isoBossController : MonoBehaviour
{
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    /// 
    [Header("BOSS | HEALTH CONFIGURATION")]
    [SerializeField]
    [Tooltip("total boss hp, gonna start at 100")]
    private int maxHp = 100;

    [SerializeField]
    [Tooltip("current health tracking")]
    private int currentHp;
    ///
    ///
    ///
    /// 
    /// 
    /// 
    [Header("BOSS | VULNERABILITY MECHANICS")]
    [SerializeField]
    [Tooltip("how many enemies player needs to kill to trigger vulnerable phase")]
    private int enemiesToKillForVuln = 5;
    
    [SerializeField]
    [Tooltip("how long boss stays vulnerable before closing up again")]
    private float vulnerableWindowDur = 10f;
    
    [SerializeField]
    [Tooltip("cooldown after closing before spawning new wave")]
    private float closedPhaseCd = 3f;
    ///
    ///
    ///
    [Header("BOSS | ENEMY SPAWNING")]
    [SerializeField]
    [Tooltip("reference to the enemy spawner component, should be on this gameobject or a child")]
    private IsometricEnemySpawner enemySpawner;
    
    [SerializeField]
    [Tooltip("how many enemies to spawn per wave")]
    private int enemiesPerWv = 3;

    [SerializeField]
    [Tooltip("delay between enemy spawns in a wave")]
    private float spawnInt = 1.5f;
    ///
    ///
    ///
    /// 
    /// 
    [Header("BOSS | DAMAGE SETTINGS")]
    [SerializeField]
    [Tooltip("damage multiplier when vulnerable, 1.0 = normal, 2.0 = double damage")]
    private float vulnerableDmgMulti = 1.0f;

    [SerializeField]
    [Tooltip("layermask for what can damage the boss")]
    private LayerMask damageLayer;
    ///
    ///
    ///
    /// 
    /// 
    /// 
    [Header("BOSS | VISUAL FEEDBACK")]
    [SerializeField]
    [Tooltip("sprite renderer for the boss, changes color based on state")]
    private SpriteRenderer bossRend;
    
    [SerializeField]
    [Tooltip("color when boss is closed/invulnerable")]
    private Color closedColor = Color.gray;
    
    [SerializeField]
    [Tooltip("color when boss is vulnerable")]
    private Color vulnerableColor = Color.red;

    [SerializeField]
    [Tooltip("color when boss is defeated")]
    private Color defeatedColor = Color.black;
    ///
    ///
    ///
    /// 
    /// 
    [Header("BOSS | ANIMATION")]
    [SerializeField]
    [Tooltip("animator for boss open/close animations")]
    private Animator bossAnim;
    
    [SerializeField]
    [Tooltip("trigger name for opening animation")]
    private string openTrigger = "Open";
    
    [SerializeField]
    [Tooltip("trigger name for closing animation")]
    private string closeTrigger = "Close";

    [SerializeField]
    [Tooltip("trigger name for defeat animation")]
    private string defeatTrigger = "Defeat";
    ///
    ///
    ///
    /// 
    /// 
    [Header("BOSS | AUDIO SOURCES")]
    [SerializeField]
    [Tooltip("main audio source for boss sounds like opening/closing/damage")]
    private AudioSource bossAudioSrc;
    
    [SerializeField]
    [Tooltip("ambient audio source for looping background sounds")]
    private AudioSource ambientAudioSrc;

    [SerializeField]
    [Tooltip("audio source for defeat/death sounds, separate for dramatic effect")]
    private AudioSource defeatAudioSrc;
    ///
    ///
    ///
    /// 
    /// 
    [Header("BOSS | AUDIO CLIPS")]
    [SerializeField]
    [Tooltip("sound when boss opens up to vulnerable state")]
    private AudioClip[] openingSfx;
    
    [SerializeField]
    [Tooltip("sound when boss closes back to invulnerable state")]
    private AudioClip[] closingSfx;
    
    [SerializeField]
    [Tooltip("sounds when boss takes damage, can have variations")]
    private AudioClip[] damageSfx;
    
    [SerializeField]
    [Tooltip("sound when boss is defeated")]
    private AudioClip[] defeatSfx;
    
    [SerializeField]
    [Tooltip("ambient loop sound while boss is alive")]
    private AudioClip ambientLoopSfx;
    
    [SerializeField]
    [Tooltip("sound when boss spawns a new wave of enemies")]
    private AudioClip[] spawnWaveSfx;

    [SerializeField]
    [Tooltip("warning sound when boss is about to close")]
    private AudioClip vulnerableWarnSfx;
    ///
    ///
    ///
    /// 
    /// 
    /// 
    [Header("BOSS | AUDIO SETTINGS")]
    [SerializeField]
    [Tooltip("volume for boss main sounds")]
    private float bossVol = 0.8f;
    
    [SerializeField]
    [Tooltip("volume for ambient loop")]
    private float ambientVol = 0.4f;
    
    [SerializeField]
    [Tooltip("volume for defeat sound")]
    private float defeatVol = 1.0f;
    
    [SerializeField]
    [Tooltip("pitch variation for damage sounds to prevent repetition")]
    private float damagePitchVar = 0.15f;

    [SerializeField]
    [Tooltip("how many seconds before vulnerable ends to play warning sound")]
    private float vulnerableWarnTime = 3f;
    ///
    ///
    ///
    /// 
    /// 
    /// 
    [Header("BOSS | UI REFERENCE")]
    [SerializeField]
    [Tooltip("reference to the boss health bar ui component")]
    private bossHealthbarForUI hpBarUI;
    ///
    ///
    ///
    /// 
    /// 
    [Header("BOSS | DEBUG")]
    [SerializeField]
    [Tooltip("show debug gizmos and logs")]
    private bool showDebug = true;
    
    /// <summary>
    /// 
    /// 
    ///had to learn enumerations again for boss states
    ///basically what phase the boss is currently in
    /// 
    /// 
    /// </summary>
    public enum BossState
    {
        Closed,Opening,Vulnerable,Closing,Defeated     
        //closed = he cant be killed and will cotninue to spawn enemies
        //opening means it will change the animation to vulnerable
        //vulnerable is the state where he can take dmg 
        //closing is the change back to the animation back to closed 
        //deafeated self explanatory, just dead
    }
    
    
    /// 
    /// 
    /// contstant tracking var all in private
    /// 
    /// 

    private BossState currentState = BossState.Closed;
    private int enemiesKilledPhase = 0;
    private float stateTimer = 0f;
    private bool isTransitioning = false;
    private Collider2D bossCol;
    private bool hasPlayedVulnWarn = false;
    
    void Start()
    {
        InitializeBoss();
    }
    
    void Update()
    {
        UpdateBossState();
        CheckVulnerableWarning();
    }
    
    /// <summary>
    /// 
    /// subscribing to enemy death events when boss is enabled
    /// 
    /// </summary>
    private void OnEnable()
    {
        Isometricenemyai.OnAnyEnemyDeath += HandleEnemyDeathEvent;
    }
    
    /// <summary>
    /// 
    /// unsubscribing from enemy death events when disabled
    /// prevents memory leaks
    /// 
    /// </summary>
    private void OnDisable()
    {
        Isometricenemyai.OnAnyEnemyDeath -= HandleEnemyDeathEvent;
    }

    /// <summary>
    /// 
    /// initializing all boss components and references
    /// auto finding components if i didnt assign them
    /// setting up audio sources
    /// 
    /// </summary>
    private void InitializeBoss()
    {
        currentHp = maxHp;

        //auto finding enemy spawner if i forgot to assign it
        if (enemySpawner == null)
        {
            enemySpawner = GetComponentInChildren<IsometricEnemySpawner>();
            if (enemySpawner == null)
            {
                Debug.LogError("[TheUntitled9] no enemy spawner found - boss cant spawn minions!");
            }
            else
            {
                Debug.Log("[TheUntitled9] found enemy spawner component");
            }
        }
        if (bossRend == null)
        {
            bossRend = GetComponent<SpriteRenderer>();
        }
        if (bossAnim == null)
        {
            bossAnim = GetComponent<Animator>();
        }

        //theres a problem with the collider maybe where the dmg is not being taken.
        //could start debugging from here.
        bossCol = GetComponent<Collider2D>();
        if (bossCol == null)
        {
            Debug.LogWarning("[boss] no collider found - might not be able to detect attacks");
        }
        if (hpBarUI == null)
        {
            hpBarUI = FindFirstObjectByType<bossHealthbarForUI>();
            if (hpBarUI == null)
            {
                Debug.LogWarning("[boss] no health bar ui found - hp wont display");
            }
        }
        //setting up audio systems here
        InitializeAudioSources();
        if (hpBarUI != null)
        {
            hpBarUI.InitializeHealthBar(maxHp);
            hpBarUI.UpdateHealthBar(currentHp, maxHp);
        }

        UpdateVisualState();
        PlayAmbientLoop();
        StartClosedPhase();

        Debug.Log($"[Untitled9] Health System made and working and HP is at {currentHp}/{maxHp} // state: {currentState}");
    }
    
    /// <summary>
    ///reusing same structure of audio system components 
    /// as the rest of the scripts
    /// 
    /// </summary>
    private void InitializeAudioSources()
    {
    
        if (bossAudioSrc == null)
        {
            AudioSource[] sources = GetComponents<AudioSource>();
            if (sources.Length > 0)
            {
                bossAudioSrc = sources[0]; //all main boss audio
            }
            else
            {
                bossAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        bossAudioSrc.playOnAwake = false;
        bossAudioSrc.spatialBlend = 0f; //2D sound for boss
        bossAudioSrc.volume = bossVol;
        
        if (ambientAudioSrc == null)
        {
            AudioSource[] sources = GetComponents<AudioSource>();
            if (sources.Length > 1)
            {
                ambientAudioSrc = sources[1]; //all ambient audio
            }
            else
            {
                ambientAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        ambientAudioSrc.playOnAwake = false;
        ambientAudioSrc.loop = true;
        ambientAudioSrc.spatialBlend = 0f;
        ambientAudioSrc.volume = ambientVol;
        
        
        if (defeatAudioSrc == null)
        {
            AudioSource[] sources = GetComponents<AudioSource>();
            if (sources.Length > 2)
            {
                defeatAudioSrc = sources[2]; //my placeholder for the death sound in case i have no time left
            }
            else
            {
                defeatAudioSrc = gameObject.AddComponent<AudioSource>();
            }
        }
        
        defeatAudioSrc.playOnAwake = false;
        defeatAudioSrc.spatialBlend = 0f;
        defeatAudioSrc.volume = defeatVol;
        Debug.Log("[Untitled9] audio sources have all been made yaay!!!");
    }
    
    /// <summary>
    /// 
    ///amibence added for the alive state of the boss
    /// 
    /// </summary>
    private void PlayAmbientLoop()
    {
        if (ambientAudioSrc == null || ambientLoopSfx == null) return;
        
        ambientAudioSrc.clip = ambientLoopSfx;
        ambientAudioSrc.Play();
        Debug.Log("[Untitled9] ambient loop started");
    }
    
    private void StopAmbientLoop()
    {
        if (ambientAudioSrc == null) return;//this is self explanatory lol 
        
        ambientAudioSrc.Stop();
        Debug.Log("[Untitled9] ambient loop stopped");
    }
    
    
    private void PlayOpeningSound() 
    {
        if (bossAudioSrc == null || openingSfx == null || openingSfx.Length == 0) return;
        
        AudioClip selectedClip = openingSfx[Random.Range(0, openingSfx.Length)];
        bossAudioSrc.PlayOneShot(selectedClip);
        Debug.Log("[Untitled 9] played opening sound");

        ///audio system is avaialable anyways for this state but didnt have enough
        /// time to gather enough epidemic royaltry free assets to fill in everything 
        /// due to ec circumstancces
    }
    
    private void PlayClosingSound()
    {
        if (bossAudioSrc == null || closingSfx == null || closingSfx.Length == 0) return;
        
        AudioClip selectedClip = closingSfx[Random.Range(0, closingSfx.Length)];
        bossAudioSrc.PlayOneShot(selectedClip);
        Debug.Log("[boss] played closing sound");
    }
    private void PlayDamageSound()
    {
        if (bossAudioSrc == null || damageSfx == null || damageSfx.Length == 0) return;
        
        AudioClip selectedClip = damageSfx[Random.Range(0, damageSfx.Length)];
        
        
        float originalPitch = bossAudioSrc.pitch;
        bossAudioSrc.pitch = 1f + Random.Range(-damagePitchVar, damagePitchVar);
        //pitch variation is important for me for consideration of developing this project
        //as a personal one for my portfolio, needed to implement a good audio system
        
        bossAudioSrc.PlayOneShot(selectedClip);
        bossAudioSrc.pitch = originalPitch; //should reset after playing hopefully
        
        Debug.Log("[Untitled9] dmg sound was played");
    }
    private void PlayDefeatSound()
    {
        if (defeatAudioSrc == null || defeatSfx == null || defeatSfx.Length == 0) return;
        
        AudioClip selectedClip = defeatSfx[Random.Range(0, defeatSfx.Length)];
        defeatAudioSrc.PlayOneShot(selectedClip);
        Debug.Log("[Untitled9] Defeat! cue the audio-");
    }
    
    private void PlaySpawnWaveSound()
    {
        if (bossAudioSrc == null || spawnWaveSfx == null || spawnWaveSfx.Length == 0) return;
        
        AudioClip selectedClip = spawnWaveSfx[Random.Range(0, spawnWaveSfx.Length)];
        bossAudioSrc.PlayOneShot(selectedClip);
        Debug.Log("[untitled9] playspawnwave() script was done here");
    }
    
   
    private void PlayVulnerableWarningSound()
    {
        if (bossAudioSrc == null || vulnerableWarnSfx == null) return;
        
        bossAudioSrc.PlayOneShot(vulnerableWarnSfx);
        Debug.Log("[Untitled9] tik tok, get your attacks in!");
    }
    
    private void CheckVulnerableWarning()
    {
        if (currentState != BossState.Vulnerable) return;
        if (hasPlayedVulnWarn) return;
        
        if (stateTimer <= vulnerableWarnTime && stateTimer > 0f)
        {
            PlayVulnerableWarningSound();
            hasPlayedVulnWarn = true;
        }
    }
    
    
    private void UpdateBossState()
    {
        if (currentState == BossState.Defeated) return;
        
        stateTimer -= Time.deltaTime;
        
        switch (currentState) //implemented a nice lil switch case here for the boss phases
        {
            case BossState.Closed:
                UpdateClosedState(); 
                break;
                
            case BossState.Opening:
                UpdateOpeningState();
                break;
                
            case BossState.Vulnerable:
                UpdateVulnerableState();
                break;
                
            case BossState.Closing:
                UpdateClosingState();
                break;
        }
    }
    
    
    private void UpdateClosedState()
    {
        //checking if enough enemies killed
        //ususally the target is set to 5-10 for me but feel free to edit it in the serialize fields if youre 
        //playing my game
        if (enemiesKilledPhase >= enemiesToKillForVuln)
        {
            TransitionToVulnerable();
        }
    }
    
    private void UpdateOpeningState()
    {
        //waiting for opening animation to finish
        if (stateTimer <= 0f)
        {
            currentState = BossState.Vulnerable;
            stateTimer = vulnerableWindowDur;
            hasPlayedVulnWarn = false; //starting my warning back to the beginning to repeat the vulnerability window
            Debug.Log($"[untitled9] you got this much time to kill him, not that hard- {vulnerableWindowDur}s");
        }
    }
    
    
    private void UpdateVulnerableState()
    {
        if (stateTimer <= 0f)
        {
            TransitionToClosed();
        }
    }
    
    private void UpdateClosingState()
    {
        
        if (stateTimer <= 0f)
        {
            StartClosedPhase();
        }
    }
    
    /// <summary>
    /// 
    /// starting closed phase: spawning new wave of enemies
    /// resetting kill counter
    /// 
    /// </summary>
    private void StartClosedPhase()
    {
        currentState = BossState.Closed;
        enemiesKilledPhase = 0;
        UpdateVisualState();
        
        //playing spawn wave sound
        PlaySpawnWaveSound();
        
        //spawning new wave of enemies
        if (enemySpawner != null)
        {
            StartCoroutine(SpawnEnemyWave());//i like to use coroutines to prevent me from calling too many functions
            //in the update messing up the games continuity. but it couldbe the bug. ill think about it later.
        }
        
        Debug.Log($"[untitled9] come on! kill {enemiesToKillForVuln} enemies to unlock the vulnerability state again!");
    }
    
    /// <summary>
    /// 
    /// transitioning to vulnerable state
    /// playing opening animation and sound
    /// 
    /// </summary>
    private void TransitionToVulnerable()
    {
        currentState = BossState.Opening;
        stateTimer = 1f;
        UpdateVisualState();
        PlayOpeningSound();
        if (bossAnim != null && !string.IsNullOrEmpty(openTrigger))
        {
            bossAnim.SetTrigger(openTrigger);
        }
        
        Debug.Log("[untitled9] animation to just let player know becoming vulnerbale");
    }
   
    private void TransitionToClosed()
    {
        currentState = BossState.Closing;
        stateTimer = 1f + closedPhaseCd; 
        //this is just handles for animations and transitions
        UpdateVisualState();
        PlayClosingSound();
        if (bossAnim != null && !string.IsNullOrEmpty(closeTrigger))
        {
            bossAnim.SetTrigger(closeTrigger);
        }
        Debug.Log("[Untitled9] ok too late, going back to invulnerable state again");
    }
    
   
    private IEnumerator SpawnEnemyWave()
    {
        Debug.Log($"[boss] spawning wave of {enemiesPerWv} enemies");
        
        for (int i = 0; i < enemiesPerWv; i++)
        {
            if (enemySpawner != null && currentState == BossState.Closed)
            {
                GameObject spawnedEnemy = enemySpawner.SpawnEnemy();
                
                if (spawnedEnemy != null)
                {
                    Debug.Log($"[boss] spawned enemy: {spawnedEnemy.name}");
                }

                yield return new WaitForSeconds(spawnInt);
                //so this just all uses coroutines and wave spawning surrounding the boss
            }
        }
    }
    
    /// <summary>
    /// 
    /// event handler when any enemy in scene dies
    /// only counting if boss is in closed state
    /// 
    /// </summary>
    private void HandleEnemyDeathEvent(GameObject deadEnemy)
    {
        if (currentState != BossState.Closed) return;
        OnEnemyKilled();
        //this should make sure they are only considered in the closed state,never in the open state or transition
        
        Debug.Log($"[Untiled9] the enemy deaths go as follows -> {deadEnemy.name}");
    }
    
    /// <summary>
    /// 
    /// called when player kills an enemy spawned by this boss
    /// incrementing kill counter
    /// 
    /// </summary>
    public void OnEnemyKilled()
    {
        if (currentState != BossState.Closed) return;
        //there will be a difference between this and the 
        //enemyspawner or load scene managers in each level that is duplicated
        //that is it is only incrememnting the kill counter based on the amount of kills 
        //you killed for the surrounding boss
        
        enemiesKilledPhase++;
        
        if (showDebug)
        {
            Debug.Log($"[untitled9] you killed this much {enemiesKilledPhase}/{enemiesToKillForVuln}");
        }
    }

    public void TakeDamage(int damage)
    //the bug could be because the damage only works in the vulnerable state, and wont transition it over maybe...
    //but no if i did that then, nah i give up.
    {
        //just some vulnerable state error management
        if (currentState != BossState.Vulnerable)
        {
            Debug.Log("[boss] INVULNERABLE - no damage taken!");
            return;
        }
        
        //carying on the same thing with the playere attack damage where 
        //damage multipliers can be applied.

        //gonna debug this if this might be the problem of stopping my original final damage
        //maybe final damage is fixed to only the context of the vulnerabledmg only and thats it.
        int finalDmg = Mathf.RoundToInt(damage * vulnerableDmgMulti);
        currentHp -= finalDmg;
        currentHp = Mathf.Max(0, currentHp);
        
        Debug.Log($"[untitled9] took {finalDmg} that much damage and has that much hp {currentHp}/{maxHp}");
        PlayDamageSound();
        
        if (hpBarUI != null)
        {
            hpBarUI.UpdateHealthBar(currentHp, maxHp);//healthbar update
        }
        
        //did we kill him :)
        if (currentHp <= 0)
        {
            OnBossDefeated();
        }
    }
    
  
    private void OnBossDefeated() //everything can be halted finally here
    {
        currentState = BossState.Defeated;
        UpdateVisualState();
        StopAmbientLoop();
        PlayDefeatSound();
        
        if (bossAnim != null && !string.IsNullOrEmpty(defeatTrigger))
        {
            bossAnim.SetTrigger(defeatTrigger);
        }
        if (enemySpawner != null)
        {
            enemySpawner.stopAllSpawning();
        }
        
        Debug.Log("[Untitled9] bye bye untitled9!");
    }
  
    private void UpdateVisualState()
    {
        if (bossRend == null) return;
        
        switch (currentState)
        {
            case BossState.Closed:
            case BossState.Closing:
                bossRend.color = closedColor;
                break;
                
            case BossState.Opening:
            case BossState.Vulnerable:
                bossRend.color = vulnerableColor;
                break;
                
            case BossState.Defeated:
                bossRend.color = defeatedColor;
                break;
        }
    }
 
    public BossState GetCurrentState() => currentState;
    public bool IsVulnerable() => currentState == BossState.Vulnerable;
    public bool IsDefeated() => currentState == BossState.Defeated;
    public int GetCurrentHealth() => currentHp;
    public int GetMaxHealth() => maxHp;
    public float GetVulnerableTimeRemaining() => currentState == BossState.Vulnerable ? stateTimer : 0f;
    
    private void OnDrawGizmosSelected()
    {
        if (!showDebug) return;
        
        //drawing state indicator
        Gizmos.color = currentState == BossState.Vulnerable ? Color.red : Color.gray;
        Gizmos.DrawWireSphere(transform.position, 0.5f); //colours here for my gizmos can detect the changes of states
        if (enemySpawner != null)
        {
            Gizmos.color = Color.yellow;
            Gizmos.DrawWireSphere(transform.position, 2f);
        }
    }
}