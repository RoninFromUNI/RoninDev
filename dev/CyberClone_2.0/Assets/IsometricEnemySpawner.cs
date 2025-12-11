using UnityEngine;
using System.Collections;
using System.Collections.Generic;

/// <summary> 
/// so i want to now to try find a way to create
/// a spawner for isometric enemies in cyberclone and
/// handle instantiation of enemy prefabs with the enemy ai script
/// 
/// i also want to automatically configure my enemies to target my player
/// and see if i can support 3 modes (wave, continuous and manual trigger)
/// </summary> 
/// 


public class IsometricEnemySpawner : MonoBehaviour

{

    [Header("ENEMY | PREFAB CONFIGURATION")]
    [SerializeField]
    [Tooltip(" an isometric enemy should be attached to this in order for my script t work, replace with level 1 or 2 or 3 if there is gonna be one , maybe not lol")]
    private GameObject enemyPrefab;

    [SerializeField]
    [Tooltip("variety of enemy prefabs here")]
    private GameObject[] enemyVar;

    [SerializeField]
    [Tooltip("use multiple prefab instead of one")]
    private bool utiliseVar = false;

    [Header("ENEMY | TARGET DWAYNE ")]
    [SerializeField]
    [Tooltip("enemies to target setup, i need to accurately have this to be at the player target at all times, had an issue with this with just utilising one fixed enemy")]
    private Transform playerTarget;

    [SerializeField]
    [Tooltip("auto tagger for the player like last scripts")]
    private string playerTag = "Player";

    [Header("ENEMY | SPAWN POSITION SETTINGS")]
    [SerializeField]
    [Tooltip("spawn at this position")]
    private bool setSpawnHere = true;

    [SerializeField]
    [Tooltip("create mulitple spawn points at {spawnpoints} positions")]
    //would be so cool if i can add the debug log to show all my spawn points accurately here
    private Transform[] spawnPoints; //the [] creates the array of spawn points

    [SerializeField]
    [Tooltip("still considering if i should use random spawn radius or not, toggle it here")]
    private bool useRandomRadius = false;

    [SerializeField]
    [Tooltip("random placement for enemy spawners")]
    private float spawnEnemiesAroundGivenRadius = 5f;

    [Header("ENEMY | SPAWN TIMING CONFIGURATIONS")]
    [SerializeField]
    [Tooltip("enable either 3 modes, spawn, wave or manual, might variate it between levels")]
    private chooseSpawnModes spawnMode = chooseSpawnModes.Continuous;

    [SerializeField]
    [Tooltip("delay betweem spawns per seconds")]
    private float spawnInterval = 2f;

    [SerializeField]
    [Tooltip("max enemeies exist from spanwer, only a limited amount, might create logic for this to be a thing that activated a next loading scene ")]
    private int maxActiveEnemies = 10;

    [SerializeField]
    [Tooltip("automatic scene load spawn")]
    private bool autoStart = true;

    [Header("ENEMY | WAVE CONFIG")]
    [SerializeField]
    [Tooltip("Enemy per wave")]
    private int enemiesPerWave = 5;

    [SerializeField]
    [Tooltip("delay between wave in seconds")]
    private float waveCooldown = 14.5f;

    [SerializeField]
    [Tooltip("total waves i need to spawn, i can set 0 to be infinite")]
    private int totalWaves = 3;

    [Header("ENEMY | SPAWNING LIMITS")]
    [SerializeField]
    [Tooltip(" total enemies to spawn before stopping")]
    private int totalSpawnLimit = 15;

    [SerializeField]
    [Tooltip("stop spawning after a time limit in seconds, really good feature idea i guess if i wanted to do some flash waves")]
    private float timeLimit = 0f;


    /// now  i think i need to set up maybe some state tracking
    private List<GameObject> activeEnemies = new List<GameObject>();
    private int totalSpawned = 0;
    private int currentWave = 0;
    private float elapsedTime = 0f;
    private bool isSpawning = false;
    private Coroutine spawnCoroutine;

    public enum chooseSpawnModes
    {
        Continuous, Wave, Manual

        //external triggers can be of use, maybe i can make dwayne press a button to accidentally release enemies
    }

    void Start()
    {
        if (playerTarget == null)
        {
            GameObject playerObj = GameObject.FindGameObjectWithTag(playerTag);
            if (playerObj != null)
            {
                playerTarget = playerObj.transform;
                Debug.Log($"[spawner] located dwayne:'{playerObj.name}'");
            }
            else
            {
                Debug.Log($"[spawner] player was not found with the tag '{playerTag}'");
                return;
            }
        }

        //this code i  beleive should help wiht validating the prefab configs
        if (!utiliseVar && enemyPrefab == null)
        {
            //assing the normal debug errors
            Debug.LogError("[spawner] no enemy prefab assigned");
            return;
        }

        if (utiliseVar && (enemyVar == null || enemyVar.Length == 0))
        {
            Debug.LogError("[spawner] no enemy vars were added to the spawner");
            return;
        }

        if (autoStart && spawnMode != chooseSpawnModes.Manual)
        {
            BeginSpawning();
        }
    }
    void Update()
    {
        if (isSpawning && timeLimit > 0f)
        {
            elapsedTime += Time.deltaTime; //need to reaearch why specifically delta time
            if (elapsedTime >= timeLimit)
            {
                stopAllSpawning();
            }
        }
        activeEnemies.RemoveAll(enemy => enemy == null); //gotta be a simpler way than this cleanup
    }

    ///now i am introucing myself to spawning coroutines based on just the spawn mode itself.
    public void BeginSpawning()
    {
        if (isSpawning)
        {
            Debug.LogWarning("[spawner] is unleashing hell");
            return;
        }

        isSpawning = true;
        switch (spawnMode)
        {
            case chooseSpawnModes.Continuous:
                spawnCoroutine = StartCoroutine(ContinuousSpawnRoutine());
                break;
            case chooseSpawnModes.Wave:
                spawnCoroutine = StartCoroutine(WaveSpawnRoutine());
                break;
            case chooseSpawnModes.Manual:
                Debug.Log("[spawner] is set on manualmode now, i can use spawnenemy() to spawn!");
                break;


        }
    }


    public void stopAllSpawning()
    {
        if (!isSpawning) return;
        isSpawning = false;
        if (spawnCoroutine != null)
        {
            StopCoroutine(spawnCoroutine);
            spawnCoroutine = null;

        }
        Debug.Log("[spawner] spawning has stopped commencing yay");
    }

    private IEnumerator ContinuousSpawnRoutine()
    {
        while (isSpawning)
        {
            //first check spawn constraints
            if (CanSpawn())
            {
                SpawnEnemy();
                yield return new WaitForSeconds(spawnInterval);
            }
            else
            {
                //wait before checking new conditions again
                yield return new WaitForSeconds(0.855f);
            }
        }
    }
    // thats continuous done, now to do manual and wave

    private IEnumerator WaveSpawnRoutine()
    {
        while (isSpawning)
        {
            currentWave++;
            Debug.Log($"[spawner] initiating wave{currentWave}");

            //enemy spawned for this wave alone
            for (int i = 0; i < enemiesPerWave; i++)
            {
                if (CanSpawn())
                {
                    SpawnEnemy();
                    yield return new WaitForSeconds(spawnInterval);

                }
            }
            if (totalWaves > 0 && currentWave >= totalWaves)
            {
                Debug.Log("[spawner] wave limit has reached!");
                stopAllSpawning();
                yield break;
            }

            //establishing here a cooldown for dwayne to recover between waves

            Debug.Log($"[spawner] wave {currentWave} completed finally! - cooldown has been activated for {waveCooldown} seconds");
            yield return new WaitForSeconds(waveCooldown); //i think yield allows these types of return news to be established
        }
    }

    private bool CanSpawn()
    {
        if (activeEnemies.Count >= maxActiveEnemies)
        {
            return false;
        }
        //need to get a check on the total spawn limit
        if (totalSpawnLimit > 0 && totalSpawned >= totalSpawnLimit)
        {
            stopAllSpawning();
            return false;
        }
        //then 
        return true;
    }

    //hopefull this would instantise a single enemy at a single calculated spawn positoon or maybe multiple
    //lets see how it oges

    public GameObject SpawnEnemy()
    {
        //prefab selection
        GameObject prefabToSpawn = utiliseVar ?
        enemyVar[Random.Range(0, enemyVar.Length)] :
        enemyPrefab;

        if (prefabToSpawn == null)
        {
            Debug.LogError("[spawner] prefab is thankfully null");
            return null;
        }

        //spawn position calculation
        Vector3 spawnPos = CalculateSpawnPosition();

        //enemmy gonna be instantiated here

        GameObject spawnedEnemy = Instantiate(prefabToSpawn, spawnPos, Quaternion.identity);
        spawnedEnemy.name = $"{prefabToSpawn.name}_{totalSpawned}";

        //finally getting too use the isometricenemyai component
        //
        //
        //
        //
        //
        //
        //
        //
        Isometricenemyai enemyAI = spawnedEnemy.GetComponent<Isometricenemyai>();
        if (enemyAI != null && playerTarget != null)
        {
            // adding a fix here cuz my target isnt set wtf

            enemyAI.SetTarget(playerTarget); 
            Debug.Log($"[spawner] spawned {spawnedEnemy.name} WithOperator isometricenemyai");
            //
            //
            //
            //
            //
        }
        else if (enemyAI == null)

        {
            Debug.LogWarning($"[spawner] {prefabToSpawn.name} missing the crucial componenet! come on! its isometricenemyai");
            //gonna create some space here to reduce my anxiety
            //
            //
            //
            //
            //

        }

        //lastly is the tracking of spawned enemy

        activeEnemies.Add(spawnedEnemy);
        totalSpawned++;

        return spawnedEnemy;
    }
    private Vector3 CalculateSpawnPosition()
    {
        Vector3 basePosition;

        //spawn points properly configured emaning 
        if (spawnPoints != null && spawnPoints.Length > 0)
        {
            Transform selectedPoint = spawnPoints[Random.Range(0, spawnPoints.Length)];
            basePosition = selectedPoint.position;
        }
        else if (setSpawnHere)
        {
            basePosition = transform.position;
        }
        else
        {
            basePosition = transform.position;
        }

        if (useRandomRadius)
        {
            Vector2 randomCircle = Random.insideUnitCircle * spawnEnemiesAroundGivenRadius;
            basePosition += new Vector3(randomCircle.x, randomCircle.y, 0f);

        }
        return basePosition;
    }


    //i think clear all enemies is next

    public void ClearAllEnemies()
    {
        foreach (GameObject enemy in activeEnemies)
        {
            if (enemy != null)
            {
                Destroy(enemy);
            }
        }
        activeEnemies.Clear();
        Debug.Log("[spawner] all enemies vanquished immedientely!");
    }

    public void ResetSpawner()
    {
        stopAllSpawning();
        ClearAllEnemies();
        totalSpawned = 0;
        currentWave = 0;
        elapsedTime = 0;
        Debug.Log("[spawner[ spawner reset]]");
    }


    //this public accessor can be useful if i want to call back to these for a coutner maybe...

    public int GetActiveEnemyCount() => activeEnemies.Count;
    public int GetTotalSpawned() => totalSpawned;
    public int GetCurrentWave() => currentWave;
    public bool IsCurrentlySpawning() => isSpawning;

    public void settingForSpawnInterval(float newInterval)
    {
        spawnInterval = Mathf.Max(0.1f, newInterval);
    }

    public void SetMaxActiveEnemiesToFight(int newFightingMax)
    {
        maxActiveEnemies = Mathf.Max(1, newFightingMax);
        ///
        /// 
        /// 
        /// 
        /// 
        /// 
    }
    //the final bit and head back up to do the other spawn mode
    public void OnDrawGizmosSelected()
    {
        //spawn radius drawing

        if (useRandomRadius)
        {
            Gizmos.color = Color.red;
            Gizmos.DrawWireSphere(transform.position, spawnEnemiesAroundGivenRadius);
        }

        //now drawing the spawn points for display in non game mode

        if (spawnPoints != null && spawnPoints.Length > 0)
        {
            Gizmos.color = Color.green;
            foreach (Transform spawnPoint in spawnPoints)
            {
                if (spawnPoint != null)
                {
                    Gizmos.DrawWireSphere(spawnPoint.position, 0.5f);
                    Gizmos.DrawLine(transform.position, spawnPoint.position);
                }
            }
        }
        Gizmos.color = Color.yellow;
        Gizmos.DrawWireCube(transform.position, Vector3.one * 0.5f);
    }
}