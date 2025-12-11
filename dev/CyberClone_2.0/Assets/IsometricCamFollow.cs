
using UnityEngine;


/// <summary> 
/// 
/// so i am gonna explain this briefly
/// this will help operate the camera smoothly for isometric games like my one called cyberclone
/// it also tracks player with customisable smoothing and boundaries
/// and it should be compatible with isometriccharactercontroller1
/// <summary> 

public class IsometricCamFollow:MonoBehaviour
{
    [Header("settings for desired target")]
    [SerializeField]
    [Tooltip("choose a character to follow with this thing")]
    private Transform target;

    [SerializeField]
    [Tooltip("if not assinged, i can autofind the player by tag this way")]
    private string playerTag = "Player";

    //these two settings above just automatically designates the character of focus for me
    //so i can easily copy and paste scenes to duplicate the levels and augment each one for better practice

    [Header("settings for how camera follows")]
    [SerializeField]
    [Tooltip("smoothness operator to make camera follow smoothly or not (between ranges 0 to 1)")]
    [Range(0f,1f)]
    private float smoothnessOperatorAdjust=0.165f;

    [SerializeField]
    [Tooltip("this should help create an offset from the player position utilising the x y and z axis, the z should always be negative")]
    //reasoning is because....well...i forgot why
    private Vector3 offset= new Vector3 (0f,0f,-10f);


    [Header("Adjust camera boundaries here")]
    [SerializeField]
    [Tooltip("enable camera boundaries to help prevent following outside the level")]
    private bool utiliseBoundaries = false; 

    [SerializeField]
    [Tooltip("min  position cam can reach")]
    //i think this is zoom in
    private float minx = -50f; 

    [SerializeField]
    [Tooltip("max x pos for cam reach")]
    private float maxx = 50f; 

    [SerializeField]
    [Tooltip("min y for y pos")]
    private float miny = -50f;

    [SerializeField]
    [Tooltip("max y for cam reach y")]
    private float maxy=50f;

    [Header("Looking ahead toggle on or off")]
    [SerializeField]
    [Tooltip("ahead of direction of player")]
    private bool toggleLookAhead = false; 

    [SerializeField]
    [Tooltip("how far ahead can they see")]
    private float lookaheadDist = 2f; 

    private Rigidbody2D targetRigidBody;

    void Start()
    {
        if (target==null)
        {
            GameObject playerObj = GameObject.FindGameObjectWithTag(playerTag);
            if (playerObj!=null)
            {
                target =playerObj.transform; 
                Debug.Log($"[camera] found player: {playerObj.name}");
            }
            else
            {
                Debug.LogError($"[camera] dwayne was not found with '{playerTag}'!");
            }
        }
        if (target!=null &toggleLookAhead)
        {
            targetRigidBody=target.GetComponent<Rigidbody2D>();
        }

        if (target!=null)
        {
            Vector3 firstPos =target.position + offset; 
            if (utiliseBoundaries)
            {
                firstPos = ClampToBoundaries(firstPos);
            }
            transform.position = firstPos;
        }
    }

    //code from start to line 102 is fine but still clamp to boundaries still doesnt work?
    //maybe lateupdate is the issue
    //i was correct, the issue was indeed lateupdate, the paramater for clamptoboundaries was set weirdly to desirepos instead of just position, which incorrectly converted the wrong thing
    // i wanted to convert position to a vector 3, not desiredpos or newpos. 

    //this should enable the rigidbody for look ahead feature
    void LateUpdate()
    {
        if (target == null) return; 


        Vector3 desiredPos = target.position + offset; 


        if (toggleLookAhead && targetRigidBody !=null)
        {
            
                Vector2 velocity = targetRigidBody.linearVelocity;
                if(velocity.magnitude >0.165f)
                {
                    Vector3 lookahead = new Vector3(velocity.x, velocity.y, 0f).normalized * lookaheadDist;
                    desiredPos += lookahead; 
                }
            }
    

            if(utiliseBoundaries)
        {
            desiredPos = ClampToBoundaries(desiredPos); 
        }
        Vector3 smoothPos = Vector3.Lerp(transform.position, desiredPos, smoothnessOperatorAdjust);
        transform.position = smoothPos;
    }

    Vector3 ClampToBoundaries(Vector3 position)
    {
        position.x = Mathf.Clamp(position.x, minx,maxx); 
        position.y = Mathf.Clamp(position.y,miny,maxy);
        return position;
    }


        //i still am getting tutorials asking i should put external controls
        //maybe its for the entire script component, lets see

    public void SetTarget(Transform newTarget)
        {
            target=newTarget;
            if(toggleLookAhead)
            {
                targetRigidBody = target.GetComponent<Rigidbody2D>();
            }
        }
    public void adjustSmootherSpeed (float newSpeed)
    {
        smoothnessOperatorAdjust = Mathf.Clamp01(newSpeed);
    }
    public void setOffset (Vector3 newOffset)
    {
        offset = newOffset;
    }
    public void tpToTarget()
    {
        if (target != null)
        {
            Vector3 newPos = target.position + offset;
            if (utiliseBoundaries)
            {
                newPos = ClampToBoundaries(newPos);
            }
            transform.position = newPos;
        }
    }

        

    void OnDrawGizmosSelected()
    {
        if(!utiliseBoundaries) return;
        Gizmos.color=Color.cyan; 

        //outlining now our boundary rectangle over here

        Vector3 bottomL=
        new Vector3 (minx,miny,transform.position.z); 
        
        Vector3 bottomR=
        new Vector3(maxx,miny,transform.position.z);

        Vector3 topR=
        new Vector3(maxx,maxy,transform.position.z);

        Vector3 topL=
        new Vector3(minx,maxy,transform.position.z); 

        Gizmos.DrawLine(bottomL,bottomR);
        Gizmos.DrawLine(bottomR,topR); 
        Gizmos.DrawLine(topR,topL);
        Gizmos.DrawLine(topL,bottomL); 

        if (target!=null)
        {
            Gizmos.color = Color.yellow; 
            Gizmos.DrawLine(target.position,target.position+offset);
            Gizmos.DrawWireSphere(target.position +offset,0.5f);
        }

    }
}

    
        